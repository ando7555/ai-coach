package com.ai.coach.domain.dto;

import com.ai.coach.domain.entity.PlayerMatchStat;

public record PlayerMatchStatEdge(PlayerMatchStat node, String cursor) {
}
