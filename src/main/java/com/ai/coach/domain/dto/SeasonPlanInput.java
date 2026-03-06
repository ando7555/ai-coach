package com.ai.coach.domain.dto;

public record SeasonPlanInput(
        Long teamId,
        String season,
        String priority
) { }