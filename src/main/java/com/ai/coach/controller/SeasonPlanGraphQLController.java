package com.ai.coach.controller;

import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.entity.SeasonPlan;
import com.ai.coach.service.SeasonPlanService;
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
public class SeasonPlanGraphQLController {

    private final SeasonPlanService seasonPlanService;

    @QueryMapping
    public List<SeasonPlan> seasonPlansByTeam(@Argument Long teamId) {
        return seasonPlanService.getByTeam(teamId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SeasonPlan generateSeasonPlan(@Argument @Valid SeasonPlanInput input) {
        return seasonPlanService.generateSeasonPlan(input);
    }
}
