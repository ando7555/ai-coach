package com.ai.coach.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlayerMatchStatInput(
        @NotNull(message = "playerId is required") Long playerId,
        @NotNull(message = "matchId is required") Long matchId,
        @Min(value = 0, message = "minutesPlayed must be at least 0")
        @Max(value = 120, message = "minutesPlayed must be at most 120")
        int minutesPlayed,
        Integer goals,
        Integer assists,
        Integer yellowCards,
        Boolean redCard
) {}
