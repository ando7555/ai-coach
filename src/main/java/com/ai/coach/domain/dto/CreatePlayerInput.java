package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record CreatePlayerInput(
        @NotNull Long teamId,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "position is required") String position,
        @DecimalMin(value = "1.0", message = "rating must be at least 1")
        @DecimalMax(value = "10.0", message = "rating must be at most 10")
        Double rating
) {}
