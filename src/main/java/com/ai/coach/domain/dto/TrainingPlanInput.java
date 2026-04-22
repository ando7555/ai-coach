package com.ai.coach.domain.dto;

import com.ai.coach.domain.entity.FocusArea;
import com.ai.coach.domain.entity.TrainingIntensity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainingPlanInput(
        @NotNull(message = "teamId is required") Long teamId,
        @NotBlank(message = "weekStart is required") String weekStart,
        @NotBlank(message = "weekEnd is required") String weekEnd,
        @NotNull(message = "primaryFocus is required") FocusArea primaryFocus,
        TrainingIntensity intensity
) { }
