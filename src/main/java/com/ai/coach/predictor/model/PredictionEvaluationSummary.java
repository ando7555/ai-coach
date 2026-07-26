package com.ai.coach.predictor.model;

import java.util.List;

public record PredictionEvaluationSummary(
        int generatedPredictions,
        int evaluatedPredictions,
        int correctWinnerPredictions,
        double winnerAccuracy,
        Double averageBrierScore,
        String modelVersion,
        List<String> notes
) {}
