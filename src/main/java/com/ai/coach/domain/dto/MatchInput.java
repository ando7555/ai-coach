package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record MatchInput(
        @NotNull(message = "homeTeamId is required") Long homeTeamId,
        @NotNull(message = "awayTeamId is required") Long awayTeamId,
        @Min(value = 0, message = "homeGoals must not be negative") Integer homeGoals,
        @Min(value = 0, message = "awayGoals must not be negative") Integer awayGoals,
        String date // ISO-8601
) {}
