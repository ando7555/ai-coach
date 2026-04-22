package com.ai.coach.domain.dto;

import java.util.List;

public record PlayerMatchStatConnection(List<PlayerMatchStatEdge> edges, PageInfo pageInfo, int totalCount) {
}
