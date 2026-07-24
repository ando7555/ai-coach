package com.ai.coach.betting;

import com.ai.coach.predictor.model.PredictionMarket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketValueServiceTest {
    private final MarketValueService service = new MarketValueService(new BettingProperties());

    @Test
    void calculatesFairOddsImpliedProbabilityAndExpectedValue() {
        MarketValueResult result = service.evaluate(new MarketValueInput(
                1L, PredictionMarket.HOME_WIN, 0.52, 2.10));

        assertThat(result.fairOdds()).isCloseTo(1.923, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.rawImpliedProbability()).isCloseTo(0.476, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.expectedValue()).isCloseTo(0.092, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.classification()).isEqualTo(ValueClassification.POTENTIAL_VALUE);
    }

    @Test
    void rejectsInvalidOddsAndProbabilities() {
        assertThatThrownBy(() -> service.evaluate(new MarketValueInput(
                null, PredictionMarket.DRAW, 0.0, 2.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("probability");

        assertThatThrownBy(() -> service.evaluate(new MarketValueInput(
                null, PredictionMarket.DRAW, 0.5, 1.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("odds");
    }
}
