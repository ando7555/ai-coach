package com.ai.coach.domain.dto;

public record TacticalContextInput(
        Long matchId,
        String focusArea,
        String style,
        String riskLevel
) {}
