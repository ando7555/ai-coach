package com.ai.coach.domain;

import com.ai.coach.domain.entity.FormIndicator;
import com.ai.coach.domain.entity.PlayerMatchStat;

import java.util.List;

/**
 * Stateless domain utility for player form calculations.
 * Uses a sliding window comparison with configurable window sizes and weights.
 */
public final class FormCalculator {

    private FormCalculator() {}

    private static final int RECENT_WINDOW = 5;
    private static final int PREVIOUS_WINDOW = 5;
    private static final double GOAL_WEIGHT = 3.0;
    private static final double ASSIST_WEIGHT = 2.0;
    private static final double RATING_WEIGHT = 0.1;
    private static final double FORM_CHANGE_THRESHOLD = 0.15;

    public static FormIndicator calculateForm(List<PlayerMatchStat> stats) {
        if (stats.size() < 2) {
            return FormIndicator.STABLE;
        }
        int recentSize = Math.min(RECENT_WINDOW, stats.size() / 2);
        int previousSize = Math.min(PREVIOUS_WINDOW, stats.size() - recentSize);

        List<PlayerMatchStat> recentWindow = stats.subList(stats.size() - recentSize, stats.size());
        List<PlayerMatchStat> previousWindow = stats.subList(stats.size() - recentSize - previousSize, stats.size() - recentSize);

        double previousScore = averageCompositeScore(previousWindow);
        double recentScore = averageCompositeScore(recentWindow);

        if (previousScore == 0 && recentScore == 0) {
            return FormIndicator.STABLE;
        }
        if (previousScore == 0) {
            return FormIndicator.IMPROVING;
        }

        double changeRatio = (recentScore - previousScore) / previousScore;
        if (changeRatio > FORM_CHANGE_THRESHOLD) {
            return FormIndicator.IMPROVING;
        } else if (changeRatio < -FORM_CHANGE_THRESHOLD) {
            return FormIndicator.DECLINING;
        }
        return FormIndicator.STABLE;
    }

    public static double averageCompositeScore(List<PlayerMatchStat> stats) {
        return stats.stream()
                .mapToDouble(s -> s.getGoals() * GOAL_WEIGHT
                        + s.getAssists() * ASSIST_WEIGHT
                        + (s.getRating() != null ? s.getRating() * RATING_WEIGHT : 0.0))
                .average()
                .orElse(0.0);
    }
}
