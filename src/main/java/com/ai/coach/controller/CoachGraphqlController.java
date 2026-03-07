package com.ai.coach.controller;

import com.ai.coach.domain.dto.*;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.entity.SeasonPlan;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.domain.repository.*;
import com.ai.coach.service.CoachService;
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
public class CoachGraphqlController {

    private final CoachService coachService;
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
        return coachService.generateMatchAnalysis(input);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public TrainingPlan generateTrainingPlan(@Argument @Valid TrainingPlanInput input) {
        return coachService.generateTrainingPlan(input);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SeasonPlan generateSeasonPlan(@Argument @Valid SeasonPlanInput input) {
        return coachService.generateSeasonPlan(input);
    }
}
