package com.ai.coach.betting;

import com.ai.coach.predictor.model.PredictionMarket;

import java.time.OffsetDateTime;
import java.util.List;

public record MarketValueResult(
        Long predictionId,
        PredictionMarket market,
        Double modelProbability,
        Double decimalOdds,
        Double fairOdds,
        Double rawImpliedProbability,
        Double expectedValue,
        ValueClassification classification,
        List<String> validationWarnings,
        OffsetDateTime evaluatedAt
) {
}
