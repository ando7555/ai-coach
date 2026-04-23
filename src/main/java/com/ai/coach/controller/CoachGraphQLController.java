package com.ai.coach.controller;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.entity.SeasonPlan;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.service.MatchAnalysisService;
import com.ai.coach.service.SeasonPlanService;
import com.ai.coach.service.TrainingPlanService;
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
public class CoachGraphQLController {

    private final MatchAnalysisService matchAnalysisService;
    private final TrainingPlanService trainingPlanService;
    private final SeasonPlanService seasonPlanService;

    // --- Queries ---

    @QueryMapping
    public List<MatchAnalysis> matchAnalysis(@Argument Long matchId) {
        return matchAnalysisService.getByMatch(matchId);
    }

    @QueryMapping
    public List<TrainingPlan> trainingPlansByTeam(@Argument Long teamId) {
        return trainingPlanService.getByTeam(teamId);
    }

    @QueryMapping
    public List<SeasonPlan> seasonPlansByTeam(@Argument Long teamId) {
        return seasonPlanService.getByTeam(teamId);
    }

    // --- Mutations ---

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public MatchAnalysis generateMatchAnalysis(@Argument @Valid MatchAnalysisInput input) {
        return matchAnalysisService.generateMatchAnalysis(input);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public TrainingPlan generateTrainingPlan(@Argument @Valid TrainingPlanInput input) {
        return trainingPlanService.generateTrainingPlan(input);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SeasonPlan generateSeasonPlan(@Argument @Valid SeasonPlanInput input) {
        return seasonPlanService.generateSeasonPlan(input);
    }
}
