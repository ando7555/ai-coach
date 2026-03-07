package com.ai.coach.controller;

import com.ai.coach.domain.dto.TacticalContextInput;
import com.ai.coach.domain.entity.Recommendation;
import com.ai.coach.domain.entity.RecommendationContextInput;
import com.ai.coach.service.RecommendationService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class RecommendationGraphQLController {

    private final RecommendationService recommendationService;

    public RecommendationGraphQLController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @QueryMapping
    public List<Recommendation> recommendationsByMatch(@Argument Long matchId) {
        return recommendationService.getByMatch(matchId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Recommendation generateRecommendation(@Argument RecommendationContextInput context) {
        return recommendationService.generateRecommendation(context);
    }
}
