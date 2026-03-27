package com.ai.coach.domain.dto;

import com.ai.coach.domain.entity.FocusArea;
import com.ai.coach.domain.entity.RiskLevel;
import com.ai.coach.domain.entity.TacticalStyle;

public record RecommendationContextInput(
        Long matchId,
        FocusArea focusArea,
        TacticalStyle style,
        RiskLevel riskLevel
) {}
