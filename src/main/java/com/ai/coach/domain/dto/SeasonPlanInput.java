package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SeasonPlanInput(
        @NotNull(message = "teamId is required") Long teamId,
        @NotBlank(message = "season is required") String season,
        String priority
) { }
