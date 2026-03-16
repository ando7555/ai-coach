package com.ai.coach.domain.dto;

public record MatchInput(
        Long homeTeamId,
        Long awayTeamId,
        Integer homeGoals,
        Integer awayGoals,
        String date // ISO-8601
) {}
