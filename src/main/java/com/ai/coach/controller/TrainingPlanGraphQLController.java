package com.ai.coach.controller;

import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.TrainingPlan;
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
public class TrainingPlanGraphQLController {

    private final TrainingPlanService trainingPlanService;

    @QueryMapping
    public List<TrainingPlan> trainingPlansByTeam(@Argument Long teamId) {
        return trainingPlanService.getByTeam(teamId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public TrainingPlan generateTrainingPlan(@Argument @Valid TrainingPlanInput input) {
        return trainingPlanService.generateTrainingPlan(input);
    }
}
