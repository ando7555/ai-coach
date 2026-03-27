package com.ai.coach.controller;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.entity.SeasonPlan;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.domain.repository.MatchAnalysisRepository;
import com.ai.coach.domain.repository.SeasonPlanRepository;
import com.ai.coach.domain.repository.TrainingPlanRepository;
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
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final SeasonPlanRepository seasonPlanRepository;

    // --- Queries ---

    @QueryMapping
    public List<MatchAnalysis> matchAnalysis(@Argument Long matchId) {
        return matchAnalysisRepository.findByMatchId(matchId);
    }

    @QueryMapping
    public List<TrainingPlan> trainingPlansByTeam(@Argument Long teamId) {
        return trainingPlanRepository.findByTeamId(teamId);
    }

    @QueryMapping
    public List<SeasonPlan> seasonPlansByTeam(@Argument Long teamId) {
        return seasonPlanRepository.findByTeamId(teamId);
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
