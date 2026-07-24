package com.ai.coach.betting;

import com.ai.coach.predictor.model.PredictionMarket;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MarketValueInput(
        Long predictionId,
        @NotNull(message = "market is required") PredictionMarket market,
        @NotNull(message = "modelProbability is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "modelProbability must be greater than 0")
        @DecimalMax(value = "1.0", message = "modelProbability must be at most 1")
        Double modelProbability,
        @NotNull(message = "decimalOdds is required")
        @DecimalMin(value = "1.0", inclusive = false, message = "decimalOdds must be greater than 1")
        Double decimalOdds
) {
}
