package com.ai.coach.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MatchAnalysisInput(
        @NotNull(message = "matchId is required") Long matchId,
        @NotBlank(message = "focusArea is required") String focusArea,
        @NotBlank(message = "style is required") String style,
        @NotBlank(message = "riskLevel is required") String riskLevel
) { }
