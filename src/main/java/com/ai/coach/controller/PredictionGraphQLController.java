package com.ai.coach.controller;

import com.ai.coach.betting.MarketValueInput;
import com.ai.coach.betting.MarketValueResult;
import com.ai.coach.betting.MarketValueService;
import com.ai.coach.predictor.MatchPredictionService;
import com.ai.coach.predictor.model.MatchPredictionPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class PredictionGraphQLController {
    private final MatchPredictionService predictionService;
    private final MarketValueService marketValueService;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public MatchPredictionPayload matchPrediction(@Argument Long matchId) {
        return predictionService.latest(matchId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<MatchPredictionPayload> matchPredictionHistory(@Argument Long matchId) {
        return predictionService.history(matchId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public MatchPredictionPayload generateMatchPrediction(@Argument Long matchId) {
        return predictionService.generate(matchId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public MarketValueResult evaluateMarketValue(@Argument @Valid MarketValueInput input) {
        return marketValueService.evaluate(input);
    }
}
