package com.ai.coach.domain.dto;

public record PlayerMatchStatInput(
        Long playerId,
        Long matchId,
        int minutesPlayed,
        Integer goals,
        Integer assists,
        Integer yellowCards,
        Boolean redCard
) {}
