package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainingPlanInput(
        @NotNull(message = "teamId is required") Long teamId,
        @NotBlank(message = "weekStart is required") String weekStart,
        @NotBlank(message = "weekEnd is required") String weekEnd,
        @NotBlank(message = "primaryFocus is required") String primaryFocus,
        String intensity
) { }
