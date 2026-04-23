package com.ai.coach.domain.dto;

import com.ai.coach.domain.entity.Match;

public record MatchEdge(Match node, String cursor) {
}
