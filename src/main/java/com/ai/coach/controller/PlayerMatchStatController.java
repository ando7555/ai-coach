package com.ai.coach.controller;

import com.ai.coach.domain.dto.PlayerMatchStatInput;
import com.ai.coach.domain.dto.PlayerPerformanceTrend;
import com.ai.coach.domain.entity.PlayerMatchStat;
import com.ai.coach.service.PlayerMatchStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public PlayerPerformanceTrend playerTrendByLastMatches(@Argument Long playerId,
                                                           @Argument int lastN) {
        return statService.getTrendByLastMatches(playerId, lastN);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public PlayerPerformanceTrend playerTrendByDateRange(@Argument Long playerId,
                                                         @Argument String from,
                                                         @Argument String to) {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd, got: " + e.getParsedString());
        }
        return statService.getTrendByDateRange(playerId, fromDate, toDate);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PlayerMatchStat recordPlayerMatchStat(@Argument @Valid PlayerMatchStatInput input) {
        return statService.record(input);
    }
}
