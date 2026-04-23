package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotNull;

public record MatchInput(
        @NotNull(message = "homeTeamId is required") Long homeTeamId,
        @NotNull(message = "awayTeamId is required") Long awayTeamId,
        Integer homeGoals,
        Integer awayGoals,
        String date // ISO-8601
) {}
