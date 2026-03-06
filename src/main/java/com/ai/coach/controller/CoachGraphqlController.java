package com.ai.coach.controller;

import com.ai.coach.domain.dto.*;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.entity.SeasonPlan;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.domain.repository.*;
import com.ai.coach.service.CoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
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
    public MatchAnalysis generateMatchAnalysis(@Argument MatchAnalysisInput input) {
        return coachService.generateMatchAnalysis(input);
    }

    @MutationMapping
    public TrainingPlan generateTrainingPlan(@Argument TrainingPlanInput input) {
        return coachService.generateTrainingPlan(input);
    }

    @MutationMapping
    public SeasonPlan generateSeasonPlan(@Argument SeasonPlanInput input) {
        return coachService.generateSeasonPlan(input);
    }
}
