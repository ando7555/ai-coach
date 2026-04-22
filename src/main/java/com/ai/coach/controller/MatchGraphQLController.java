package com.ai.coach.controller;

import com.ai.coach.domain.dto.MatchConnection;
import com.ai.coach.domain.dto.MatchInput;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MatchGraphQLController {

    private final MatchService matchService;

    @QueryMapping
    public Match match(@Argument Long id) {
        return matchService.getMatch(id);
    }

    @QueryMapping
    public MatchConnection matchesByTeam(@Argument Long teamId,
                                         @Argument Integer first,
                                         @Argument String after) {
        return matchService.getMatchesByTeam(teamId, first, after);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Match recordMatch(@Argument MatchInput input) {
        return matchService.recordMatch(input);
    }
}
