package com.ai.coach.predictor.evaluation;

import org.springframework.stereotype.Component;

@Component
public class ProbabilityValidator {
    public void requireProbability(Double probability, String fieldName) {
        if (probability == null || probability.isNaN() || probability.isInfinite()
                || probability < 0.0 || probability > 1.0) {
            throw new IllegalStateException("%s must be a finite probability between 0 and 1".formatted(fieldName));
        }
    }

    public void requireNormalized(double tolerance, double... probabilities) {
        double sum = 0;
        for (double probability : probabilities) {
            requireProbability(probability, "probability");
            sum += probability;
        }
        if (Math.abs(1.0 - sum) > tolerance) {
            throw new IllegalStateException("Mutually exclusive probabilities must sum to 1 within tolerance");
        }
    }
}
