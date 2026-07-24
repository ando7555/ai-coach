package com.ai.coach.predictor;

import com.ai.coach.predictor.evaluation.ProbabilityValidator;
import com.ai.coach.predictor.feature.MatchFeatureSnapshot;
import com.ai.coach.predictor.feature.TeamFeatureSummary;
import com.ai.coach.predictor.model.ConfidenceLevel;
import com.ai.coach.predictor.model.DataQualityStatus;
import com.ai.coach.predictor.model.MatchPredictionResult;
import com.ai.coach.predictor.model.UncertaintyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoissonBaselineMatchPredictor implements MatchPredictor {
    private final PredictorProperties properties;
    private final ProbabilityValidator probabilityValidator;

    @Override
    public MatchPredictionResult predict(MatchFeatureSnapshot snapshot) {
        OffsetDateTime generatedAt = OffsetDateTime.now();
        if (snapshot.dataQualityStatus() == DataQualityStatus.INSUFFICIENT) {
            return insufficient(snapshot, generatedAt);
        }

        double expectedHomeGoals = calculateExpectedHomeGoals(snapshot);
        double expectedAwayGoals = calculateExpectedAwayGoals(snapshot);
        ScoreMatrix matrix = buildScoreMatrix(expectedHomeGoals, expectedAwayGoals, properties.getMaxGoals());

        probabilityValidator.requireNormalized(properties.getNormalizationTolerance(),
                matrix.homeWinProbability(), matrix.drawProbability(), matrix.awayWinProbability());

        double topProbability = List.of(matrix.homeWinProbability(), matrix.drawProbability(), matrix.awayWinProbability())
                .stream().max(Comparator.naturalOrder()).orElse(0.0);
        double secondProbability = List.of(matrix.homeWinProbability(), matrix.drawProbability(), matrix.awayWinProbability())
                .stream().sorted(Comparator.reverseOrder()).skip(1).findFirst().orElse(0.0);
        double separation = topProbability - secondProbability;
        ConfidenceLevel confidence = confidence(snapshot, separation);
        UncertaintyLevel uncertainty = switch (confidence) {
            case HIGH -> UncertaintyLevel.LOW;
            case MEDIUM -> UncertaintyLevel.MEDIUM;
            case LOW -> UncertaintyLevel.HIGH;
        };

        List<String> factors = new ArrayList<>();
        factors.add("Prediction uses completed matches before %s only".formatted(snapshot.cutoffTimestamp()));
        factors.add("%s sample: %d matches; %s sample: %d matches".formatted(
                snapshot.homeTeamName(), snapshot.homeTeam().totalMatches(),
                snapshot.awayTeamName(), snapshot.awayTeam().totalMatches()));
        factors.add("Expected goals: %s %.2f, %s %.2f".formatted(
                snapshot.homeTeamName(), expectedHomeGoals,
                snapshot.awayTeamName(), expectedAwayGoals));
        if (snapshot.dataQualityStatus() == DataQualityStatus.LIMITED) {
            factors.add("Venue split is limited, so total team form receives more weight");
        }

        return new MatchPredictionResult(
                snapshot.matchId(),
                snapshot.homeTeamId(),
                snapshot.awayTeamId(),
                matrix.homeWinProbability(),
                matrix.drawProbability(),
                matrix.awayWinProbability(),
                expectedHomeGoals,
                expectedAwayGoals,
                matrix.bothTeamsToScoreProbability(),
                matrix.over25GoalsProbability(),
                matrix.under25GoalsProbability(),
                matrix.mostLikelyScore(),
                confidence,
                uncertainty,
                snapshot.dataQualityStatus(),
                List.copyOf(factors),
                snapshot.missingFeatures(),
                properties.getModelName(),
                properties.getModelVersion(),
                generatedAt
        );
    }

    public ScoreMatrix buildScoreMatrix(double expectedHomeGoals, double expectedAwayGoals, int maxGoals) {
        double[] home = poissonDistribution(expectedHomeGoals, maxGoals);
        double[] away = poissonDistribution(expectedAwayGoals, maxGoals);
        double rawTotal = 0;
        for (double h : home) {
            for (double a : away) {
                rawTotal += h * a;
            }
        }

        double homeWin = 0;
        double draw = 0;
        double awayWin = 0;
        double over25 = 0;
        double bothScore = 0;
        double mostLikely = -1;
        String mostLikelyScore = "0-0";

        for (int h = 0; h <= maxGoals; h++) {
            for (int a = 0; a <= maxGoals; a++) {
                double probability = (home[h] * away[a]) / rawTotal;
                if (h > a) {
                    homeWin += probability;
                } else if (h == a) {
                    draw += probability;
                } else {
                    awayWin += probability;
                }
                if (h + a > 2.5) {
                    over25 += probability;
                }
                if (h > 0 && a > 0) {
                    bothScore += probability;
                }
                if (probability > mostLikely) {
                    mostLikely = probability;
                    mostLikelyScore = h + "-" + a;
                }
            }
        }
        double under25 = 1.0 - over25;
        return new ScoreMatrix(homeWin, draw, awayWin, bothScore, over25, under25, mostLikelyScore);
    }

    double calculateExpectedHomeGoals(MatchFeatureSnapshot snapshot) {
        TeamFeatureSummary home = snapshot.homeTeam();
        TeamFeatureSummary away = snapshot.awayTeam();
        double homeAttack = blend(
                home.homeMatches() >= properties.getMinVenueMatches() ? home.homeGoalsForPerMatch() : home.goalsForPerMatch(),
                home.recentGoalsForPerMatch());
        double awayDefence = blend(
                away.awayMatches() >= properties.getMinVenueMatches() ? away.awayGoalsAgainstPerMatch() : away.goalsAgainstPerMatch(),
                away.recentGoalsAgainstPerMatch());
        double expected = (homeAttack + awayDefence + snapshot.globalHomeGoalsPerMatch()) / 3.0;
        return clamp(expected);
    }

    double calculateExpectedAwayGoals(MatchFeatureSnapshot snapshot) {
        TeamFeatureSummary home = snapshot.homeTeam();
        TeamFeatureSummary away = snapshot.awayTeam();
        double awayAttack = blend(
                away.awayMatches() >= properties.getMinVenueMatches() ? away.awayGoalsForPerMatch() : away.goalsForPerMatch(),
                away.recentGoalsForPerMatch());
        double homeDefence = blend(
                home.homeMatches() >= properties.getMinVenueMatches() ? home.homeGoalsAgainstPerMatch() : home.goalsAgainstPerMatch(),
                home.recentGoalsAgainstPerMatch());
        double expected = (awayAttack + homeDefence + snapshot.globalAwayGoalsPerMatch()) / 3.0;
        return clamp(expected);
    }

    private MatchPredictionResult insufficient(MatchFeatureSnapshot snapshot, OffsetDateTime generatedAt) {
        List<String> factors = new ArrayList<>();
        factors.add("Insufficient completed historical data before %s".formatted(snapshot.cutoffTimestamp()));
        factors.addAll(snapshot.missingFeatures());
        return new MatchPredictionResult(
                snapshot.matchId(),
                snapshot.homeTeamId(),
                snapshot.awayTeamId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ConfidenceLevel.LOW,
                UncertaintyLevel.HIGH,
                DataQualityStatus.INSUFFICIENT,
                List.copyOf(factors),
                snapshot.missingFeatures(),
                properties.getModelName(),
                properties.getModelVersion(),
                generatedAt
        );
    }

    private double[] poissonDistribution(double lambda, int maxGoals) {
        double[] values = new double[maxGoals + 1];
        values[0] = Math.exp(-lambda);
        for (int goals = 1; goals <= maxGoals; goals++) {
            values[goals] = values[goals - 1] * lambda / goals;
        }
        return values;
    }

    private double blend(double baseline, double recent) {
        if (recent <= 0) {
            return baseline;
        }
        return (baseline * (1.0 - properties.getRecentWeight())) + (recent * properties.getRecentWeight());
    }

    private double clamp(double value) {
        return Math.max(properties.getMinExpectedGoals(), Math.min(properties.getMaxExpectedGoals(), value));
    }

    private ConfidenceLevel confidence(MatchFeatureSnapshot snapshot, double separation) {
        int minSample = Math.min(snapshot.homeTeam().totalMatches(), snapshot.awayTeam().totalMatches());
        if (minSample >= properties.getHighConfidenceSampleSize()
                && separation >= properties.getHighSeparationThreshold()
                && snapshot.dataQualityStatus() == DataQualityStatus.SUFFICIENT) {
            return ConfidenceLevel.HIGH;
        }
        if (minSample >= properties.getMediumConfidenceSampleSize()
                && separation >= properties.getMediumSeparationThreshold()) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }

    public record ScoreMatrix(
            double homeWinProbability,
            double drawProbability,
            double awayWinProbability,
            double bothTeamsToScoreProbability,
            double over25GoalsProbability,
            double under25GoalsProbability,
            String mostLikelyScore
    ) {
    }
}
