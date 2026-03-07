package com.ai.coach.domain.dto;

import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.PlayerMatchStat;

import java.util.List;

public record PlayerPerformanceTrend(
        Player player,
        int matchCount,
        int totalGoals,
        int totalAssists,
        int totalGoalContributions,
        double averageGoals,
        double averageAssists,
        double averageGoalContributions,
        Double averageRating,
        int totalMinutesPlayed,
        FormIndicator form,
        List<PlayerMatchStat> matches
) {}
