package com.ai.coach.domain.dto;

import com.ai.coach.domain.entity.FocusArea;
import com.ai.coach.domain.entity.RiskLevel;
import com.ai.coach.domain.entity.TacticalStyle;
import jakarta.validation.constraints.NotNull;

public record RecommendationContextInput(
        @NotNull(message = "matchId is required") Long matchId,
        @NotNull(message = "focusArea is required") FocusArea focusArea,
        @NotNull(message = "style is required") TacticalStyle style,
        @NotNull(message = "riskLevel is required") RiskLevel riskLevel
) {}
