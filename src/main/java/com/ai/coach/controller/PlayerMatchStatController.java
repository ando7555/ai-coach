package com.ai.coach.controller;

import com.ai.coach.domain.dto.PlayerMatchStatInput;
import com.ai.coach.domain.entity.PlayerMatchStat;
import com.ai.coach.service.PlayerMatchStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class PlayerMatchStatController {

    private final PlayerMatchStatService statService;

    @QueryMapping
    public List<PlayerMatchStat> statsByMatch(@Argument Long matchId) {
        return statService.getByMatch(matchId);
    }

    @QueryMapping
    public List<PlayerMatchStat> statsByPlayer(@Argument Long playerId) {
        return statService.getByPlayer(playerId);
    }

    @MutationMapping
    public PlayerMatchStat recordPlayerMatchStat(@Argument @Valid PlayerMatchStatInput input) {
        return statService.record(input);
    }
}
