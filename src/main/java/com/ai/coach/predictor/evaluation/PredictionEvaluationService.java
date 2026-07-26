package com.ai.coach.predictor.evaluation;

import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.MatchPredictionRecord;
import com.ai.coach.domain.repository.MatchPredictionRecordRepository;
import com.ai.coach.predictor.model.DataQualityStatus;
import com.ai.coach.predictor.model.PredictionEvaluationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionEvaluationService {
    private final MatchPredictionRecordRepository predictionRepository;

    @Transactional(readOnly = true)
    public PredictionEvaluationSummary summarizeLatestCompletedPredictions() {
        List<MatchPredictionRecord> all = predictionRepository.findAll();
        Map<Long, MatchPredictionRecord> latestByMatch = latestByMatch(all);
        int evaluated = 0;
        int correct = 0;
        double brierSum = 0;
        String modelVersion = null;

        for (MatchPredictionRecord prediction : latestByMatch.values()) {
            Match match = prediction.getMatch();
            if (!canEvaluate(prediction, match)) {
                continue;
            }
            evaluated++;
            String actual = actualOutcome(match);
            String predicted = predictedOutcome(prediction);
            if (actual.equals(predicted)) {
                correct++;
            }
            brierSum += brierScore(prediction, actual);
            if (modelVersion == null && prediction.getModelVersion() != null) {
                modelVersion = prediction.getModelVersion();
            }
        }

        List<String> notes = new ArrayList<>();
        notes.add("Evaluation uses the latest saved prediction per match with a completed result.");
        notes.add("Brier score covers home/draw/away probabilities only; lower is better.");
        if (evaluated == 0) {
            notes.add("No completed predicted matches are available yet. Record final scores for predicted fixtures to unlock evaluation.");
        }

        return new PredictionEvaluationSummary(
                all.size(),
                evaluated,
                correct,
                evaluated == 0 ? 0 : (double) correct / evaluated,
                evaluated == 0 ? null : brierSum / evaluated,
                modelVersion,
                List.copyOf(notes)
        );
    }

    private Map<Long, MatchPredictionRecord> latestByMatch(List<MatchPredictionRecord> predictions) {
        Map<Long, MatchPredictionRecord> latest = new HashMap<>();
        for (MatchPredictionRecord prediction : predictions) {
            Match match = prediction.getMatch();
            if (match == null || match.getId() == null) {
                continue;
            }
            latest.merge(match.getId(), prediction, (current, candidate) ->
                    Comparator.nullsFirst(Comparator.<java.time.OffsetDateTime>naturalOrder())
                            .compare(current.getGeneratedAt(), candidate.getGeneratedAt()) >= 0 ? current : candidate);
        }
        return latest;
    }

    private boolean canEvaluate(MatchPredictionRecord prediction, Match match) {
        return match != null
                && match.getHomeGoals() != null
                && match.getAwayGoals() != null
                && prediction.getDataQualityStatus() != DataQualityStatus.INSUFFICIENT
                && prediction.getHomeWinProbability() != null
                && prediction.getDrawProbability() != null
                && prediction.getAwayWinProbability() != null;
    }

    private String actualOutcome(Match match) {
        if (match.getHomeGoals() > match.getAwayGoals()) {
            return "HOME";
        }
        if (match.getHomeGoals() < match.getAwayGoals()) {
            return "AWAY";
        }
        return "DRAW";
    }

    private String predictedOutcome(MatchPredictionRecord prediction) {
        double home = prediction.getHomeWinProbability();
        double draw = prediction.getDrawProbability();
        double away = prediction.getAwayWinProbability();
        if (home >= draw && home >= away) {
            return "HOME";
        }
        if (away >= home && away >= draw) {
            return "AWAY";
        }
        return "DRAW";
    }

    private double brierScore(MatchPredictionRecord prediction, String actual) {
        return (square(prediction.getHomeWinProbability() - target(actual, "HOME"))
                + square(prediction.getDrawProbability() - target(actual, "DRAW"))
                + square(prediction.getAwayWinProbability() - target(actual, "AWAY"))) / 3.0;
    }

    private double target(String actual, String outcome) {
        return actual.equals(outcome) ? 1.0 : 0.0;
    }

    private double square(double value) {
        return value * value;
    }
}
