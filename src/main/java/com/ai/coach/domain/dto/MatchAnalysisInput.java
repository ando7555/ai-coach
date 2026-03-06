package com.ai.coach.domain.dto;

public record MatchAnalysisInput(
        Long matchId,
        String focusArea,
        String style,
        String riskLevel
) { }