package com.ai.coach.domain;

import com.ai.coach.domain.entity.PlayerMatchStat;

/**
 * Accumulates player match statistics for trend calculation.
 * Domain utility — sits alongside FormCalculator and WorkloadCalculator.
 */
public record StatAggregator(int goals, int assists, int minutes, double ratingSum, int ratedCount) {

    public static final StatAggregator IDENTITY = new StatAggregator(0, 0, 0, 0.0, 0);

    public StatAggregator add(PlayerMatchStat s) {
        double r = s.getRating() != null ? s.getRating() : 0.0;
        int rc = s.getRating() != null ? 1 : 0;
        return new StatAggregator(goals + s.getGoals(), assists + s.getAssists(),
                minutes + s.getMinutesPlayed(), ratingSum + r, ratedCount + rc);
    }

    public StatAggregator merge(StatAggregator other) {
        return new StatAggregator(goals + other.goals, assists + other.assists,
                minutes + other.minutes, ratingSum + other.ratingSum, ratedCount + other.ratedCount);
    }
}
