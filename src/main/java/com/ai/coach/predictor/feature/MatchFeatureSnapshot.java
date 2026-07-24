package com.ai.coach.predictor.feature;

import com.ai.coach.predictor.model.DataQualityStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record MatchFeatureSnapshot(
        Long matchId,
        Long homeTeamId,
        Long awayTeamId,
        String homeTeamName,
        String awayTeamName,
        LocalDate matchDate,
        OffsetDateTime cutoffTimestamp,
        TeamFeatureSummary homeTeam,
        TeamFeatureSummary awayTeam,
        int globalCompletedMatches,
        double globalHomeGoalsPerMatch,
        double globalAwayGoalsPerMatch,
        List<String> availableFeatures,
        List<String> missingFeatures,
        DataQualityStatus dataQualityStatus
) {
}
