package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePlayerInput(
        @NotNull Long teamId,
        @NotNull String name,
        @NotNull String position,
        Double rating
) {}
