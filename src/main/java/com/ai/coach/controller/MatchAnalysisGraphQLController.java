package com.ai.coach.controller;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.service.MatchAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class MatchAnalysisGraphQLController {

    private final MatchAnalysisService matchAnalysisService;

    @QueryMapping
    public List<MatchAnalysis> matchAnalysis(@Argument Long matchId) {
        return matchAnalysisService.getByMatch(matchId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public MatchAnalysis generateMatchAnalysis(@Argument @Valid MatchAnalysisInput input) {
        return matchAnalysisService.generateMatchAnalysis(input);
    }
}
