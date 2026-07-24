package com.ai.coach.predictor;

import com.ai.coach.predictor.evaluation.ProbabilityValidator;
import com.ai.coach.predictor.feature.MatchFeatureSnapshot;
import com.ai.coach.predictor.feature.TeamFeatureSummary;
import com.ai.coach.predictor.model.DataQualityStatus;
import com.ai.coach.predictor.model.MatchPredictionResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PoissonBaselineMatchPredictorTest {
    private final PredictorProperties properties = new PredictorProperties();
    private final PoissonBaselineMatchPredictor predictor =
            new PoissonBaselineMatchPredictor(properties, new ProbabilityValidator());

    @Test
    void probabilitiesAreBoundedAndNormalized() {
        MatchPredictionResult result = predictor.predict(snapshot(DataQualityStatus.SUFFICIENT));

        assertThat(result.homeWinProbability()).isBetween(0.0, 1.0);
        assertThat(result.drawProbability()).isBetween(0.0, 1.0);
        assertThat(result.awayWinProbability()).isBetween(0.0, 1.0);
        assertThat(result.homeWinProbability() + result.drawProbability() + result.awayWinProbability())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test
    void predictionIsDeterministicExceptGeneratedTimestamp() {
        MatchFeatureSnapshot snapshot = snapshot(DataQualityStatus.SUFFICIENT);

        MatchPredictionResult first = predictor.predict(snapshot);
        MatchPredictionResult second = predictor.predict(snapshot);

        assertThat(second.homeWinProbability()).isEqualTo(first.homeWinProbability());
        assertThat(second.drawProbability()).isEqualTo(first.drawProbability());
        assertThat(second.awayWinProbability()).isEqualTo(first.awayWinProbability());
        assertThat(second.expectedHomeGoals()).isEqualTo(first.expectedHomeGoals());
        assertThat(second.mostLikelyScore()).isEqualTo(first.mostLikelyScore());
    }

    @Test
    void scoreMatrixCalculatesOverUnderAndBtts() {
        PoissonBaselineMatchPredictor.ScoreMatrix matrix = predictor.buildScoreMatrix(1.6, 1.1, 8);

        assertThat(matrix.over25GoalsProbability()).isBetween(0.0, 1.0);
        assertThat(matrix.under25GoalsProbability()).isCloseTo(1.0 - matrix.over25GoalsProbability(),
                org.assertj.core.data.Offset.offset(0.000001));
        assertThat(matrix.bothTeamsToScoreProbability()).isBetween(0.0, 1.0);
        assertThat(matrix.mostLikelyScore()).contains("-");
    }

    @Test
    void insufficientDataDoesNotProduceProbabilityPrecision() {
        MatchPredictionResult result = predictor.predict(snapshot(DataQualityStatus.INSUFFICIENT));

        assertThat(result.homeWinProbability()).isNull();
        assertThat(result.expectedHomeGoals()).isNull();
        assertThat(result.dataQualityStatus()).isEqualTo(DataQualityStatus.INSUFFICIENT);
        assertThat(result.warnings()).isNotEmpty();
    }

    private MatchFeatureSnapshot snapshot(DataQualityStatus status) {
        TeamFeatureSummary home = new TeamFeatureSummary(1L, "Home", 8, 4, 4,
                1.7, 1.0, 1.9, 0.8, 1.5, 1.2, 1.8, 0.9);
        TeamFeatureSummary away = new TeamFeatureSummary(2L, "Away", 8, 4, 4,
                1.2, 1.4, 1.4, 1.2, 1.0, 1.6, 1.1, 1.5);
        return new MatchFeatureSnapshot(
                20L,
                1L,
                2L,
                "Home",
                "Away",
                LocalDate.of(2026, 8, 1),
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                home,
                away,
                12,
                1.45,
                1.20,
                List.of("history"),
                status == DataQualityStatus.INSUFFICIENT ? List.of("missing history") : List.of(),
                status);
    }
}
