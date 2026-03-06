package com.ai.coach.controller;


import com.ai.coach.domain.dto.MatchInput;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.service.MatchService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class MatchGraphQLController {

    private final MatchService matchService;

    public MatchGraphQLController(MatchService matchService) {
        this.matchService = matchService;
    }

    @QueryMapping
    public Match match(@Argument Long id) {
        return matchService.getMatch(id);
    }

    @QueryMapping
    public List<Match> matchesByTeam(@Argument Long teamId) {
        return matchService.getMatchesByTeam(teamId);
    }

    @MutationMapping
    public Match recordMatch(@Argument MatchInput input) {
        return matchService.recordMatch(input);
    }
}
