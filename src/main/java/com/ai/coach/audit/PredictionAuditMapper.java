package com.ai.coach.audit;

import com.ai.coach.domain.entity.MatchPredictionRecord;
import com.ai.coach.predictor.model.MatchPredictionPayload;
import org.springframework.stereotype.Component;

@Component
public class PredictionAuditMapper {
    public MatchPredictionPayload toPayload(MatchPredictionRecord record) {
        return new MatchPredictionPayload(
                record.getId(),
                record.getMatch().getId(),
                record.getMatch().getHomeTeam().getId(),
                record.getMatch().getAwayTeam().getId(),
                round(record.getHomeWinProbability(), 4),
                round(record.getDrawProbability(), 4),
                round(record.getAwayWinProbability(), 4),
                round(record.getExpectedHomeGoals(), 3),
                round(record.getExpectedAwayGoals(), 3),
                round(record.getBothTeamsToScoreProbability(), 4),
                round(record.getOver25GoalsProbability(), 4),
                round(record.getUnder25GoalsProbability(), 4),
                record.getMostLikelyScore(),
                record.getConfidenceLevel(),
                record.getUncertaintyLevel(),
                record.getDataQualityStatus(),
                record.getExplanationFactors(),
                record.getWarnings(),
                record.getModelName(),
                record.getModelVersion(),
                record.getPredictionVersion(),
                record.getFeatureCutoffTimestamp(),
                record.getFeatureSummary(),
                record.getGeneratedAt()
        );
    }

    private Double round(Double value, int places) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
