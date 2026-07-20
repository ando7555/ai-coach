package com.ai.coach.predictor;

import com.ai.coach.audit.PredictionAuditMapper;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.MatchPredictionRecord;
import com.ai.coach.domain.repository.MatchPredictionRecordRepository;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.predictor.feature.MatchFeatureExtractor;
import com.ai.coach.predictor.feature.MatchFeatureSnapshot;
import com.ai.coach.predictor.model.MatchPredictionPayload;
import com.ai.coach.predictor.model.MatchPredictionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchPredictionService {
    private final MatchRepository matchRepository;
    private final MatchPredictionRecordRepository predictionRepository;
    private final MatchFeatureExtractor featureExtractor;
    private final MatchPredictor matchPredictor;
    private final PredictionAuditMapper mapper;

    @Transactional
    public MatchPredictionPayload generate(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match", matchId));
        MatchFeatureSnapshot snapshot = featureExtractor.extract(match, matchRepository.findAll());
        MatchPredictionResult result = matchPredictor.predict(snapshot);
        int version = predictionRepository.findByMatchId(matchId).stream()
                .map(MatchPredictionRecord::getPredictionVersion)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        MatchPredictionRecord record = MatchPredictionRecord.builder()
                .match(match)
                .featureCutoffTimestamp(snapshot.cutoffTimestamp())
                .featureSummary(featureSummary(snapshot))
                .homeWinProbability(result.homeWinProbability())
                .drawProbability(result.drawProbability())
                .awayWinProbability(result.awayWinProbability())
                .expectedHomeGoals(result.expectedHomeGoals())
                .expectedAwayGoals(result.expectedAwayGoals())
                .bothTeamsToScoreProbability(result.bothTeamsToScoreProbability())
                .over25GoalsProbability(result.over25GoalsProbability())
                .under25GoalsProbability(result.under25GoalsProbability())
                .mostLikelyScore(result.mostLikelyScore())
                .confidenceLevel(result.confidenceLevel())
                .uncertaintyLevel(result.uncertaintyLevel())
                .dataQualityStatus(result.dataQualityStatus())
                .explanationFactors(result.explanationFactors())
                .warnings(result.warnings())
                .modelName(result.modelName())
                .modelVersion(result.modelVersion())
                .predictionVersion(version)
                .generatedAt(result.generatedAt())
                .build();
        return mapper.toPayload(predictionRepository.save(record));
    }

    @Transactional(readOnly = true)
    public MatchPredictionPayload latest(Long matchId) {
        return predictionRepository.findFirstByMatchIdOrderByGeneratedAtDesc(matchId)
                .map(mapper::toPayload)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MatchPredictionPayload> history(Long matchId) {
        return predictionRepository.findByMatchId(matchId).stream()
                .sorted(Comparator.comparing(MatchPredictionRecord::getGeneratedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(mapper::toPayload)
                .toList();
    }

    private String featureSummary(MatchFeatureSnapshot snapshot) {
        return """
                cutoff=%s; globalCompletedMatches=%d; homeTeam=%s sample=%d homeSample=%d; awayTeam=%s sample=%d awaySample=%d; dataQuality=%s; missing=%s
                """.formatted(
                snapshot.cutoffTimestamp(),
                snapshot.globalCompletedMatches(),
                snapshot.homeTeamName(),
                snapshot.homeTeam().totalMatches(),
                snapshot.homeTeam().homeMatches(),
                snapshot.awayTeamName(),
                snapshot.awayTeam().totalMatches(),
                snapshot.awayTeam().awayMatches(),
                snapshot.dataQualityStatus(),
                String.join(" | ", snapshot.missingFeatures())
        ).trim();
    }
}
