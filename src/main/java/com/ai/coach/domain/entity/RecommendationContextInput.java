package com.ai.coach.domain.entity;

public record RecommendationContextInput(
        Long matchId,
        FocusArea focusArea,
        TacticalStyle style,
        RiskLevel riskLevel
) {}
