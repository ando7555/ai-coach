package com.ai.coach.domain.dto;

public record TrainingPlanInput(
        Long teamId,
        String weekStart,
        String weekEnd,
        String primaryFocus,
        String intensity
) { }