package com.ai.coach.predictor.feature;

public record TeamFeatureSummary(
        Long teamId,
        String teamName,
        int totalMatches,
        int homeMatches,
        int awayMatches,
        double goalsForPerMatch,
        double goalsAgainstPerMatch,
        double homeGoalsForPerMatch,
        double homeGoalsAgainstPerMatch,
        double awayGoalsForPerMatch,
        double awayGoalsAgainstPerMatch,
        double recentGoalsForPerMatch,
        double recentGoalsAgainstPerMatch
) {
}
