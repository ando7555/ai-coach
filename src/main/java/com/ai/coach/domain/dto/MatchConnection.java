package com.ai.coach.domain.dto;

import java.util.List;

public record MatchConnection(List<MatchEdge> edges, PageInfo pageInfo, int totalCount) {
}
