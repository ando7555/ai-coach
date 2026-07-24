package com.ai.coach.predictor.model;

import java.time.OffsetDateTime;
import java.util.List;

public record MatchPredictionResult(
        Long matchId,
        Long homeTeamId,
        Long awayTeamId,
        Double homeWinProbability,
        Double drawProbability,
        Double awayWinProbability,
        Double expectedHomeGoals,
        Double expectedAwayGoals,
        Double bothTeamsToScoreProbability,
        Double over25GoalsProbability,
        Double under25GoalsProbability,
        String mostLikelyScore,
        ConfidenceLevel confidenceLevel,
        UncertaintyLevel uncertaintyLevel,
        DataQualityStatus dataQualityStatus,
        List<String> explanationFactors,
        List<String> warnings,
        String modelName,
        String modelVersion,
        OffsetDateTime generatedAt
) {
}
