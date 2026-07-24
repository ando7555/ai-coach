package com.ai.coach.betting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketValueService {
    private final BettingProperties properties;

    public MarketValueResult evaluate(MarketValueInput input) {
        validate(input);
        double fairOdds = 1.0 / input.modelProbability();
        double impliedProbability = 1.0 / input.decimalOdds();
        double expectedValue = (input.modelProbability() * input.decimalOdds()) - 1.0;

        List<String> warnings = new ArrayList<>();
        ValueClassification classification;
        if (input.modelProbability() <= properties.getHighUncertaintyProbabilityThreshold()) {
            classification = ValueClassification.HIGH_UNCERTAINTY;
            warnings.add("Model probability is very low; treat value output as high uncertainty.");
        } else if (expectedValue >= properties.getPotentialValueThreshold()) {
            classification = ValueClassification.POTENTIAL_VALUE;
        } else if (expectedValue > properties.getWeakValueThreshold()) {
            classification = ValueClassification.WEAK_VALUE;
        } else {
            classification = ValueClassification.NO_VALUE;
        }

        return new MarketValueResult(
                input.predictionId(),
                input.market(),
                input.modelProbability(),
                input.decimalOdds(),
                fairOdds,
                impliedProbability,
                expectedValue,
                classification,
                List.copyOf(warnings),
                OffsetDateTime.now()
        );
    }

    private void validate(MarketValueInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Market value input is required");
        }
        if (input.market() == null) {
            throw new IllegalArgumentException("Market is required");
        }
        if (input.modelProbability() == null || input.modelProbability().isNaN()
                || input.modelProbability().isInfinite() || input.modelProbability() <= 0
                || input.modelProbability() > 1) {
            throw new IllegalArgumentException("Model probability must be greater than 0 and at most 1");
        }
        if (input.decimalOdds() == null || input.decimalOdds().isNaN()
                || input.decimalOdds().isInfinite() || input.decimalOdds() <= 1) {
            throw new IllegalArgumentException("Decimal odds must be greater than 1");
        }
    }
}
