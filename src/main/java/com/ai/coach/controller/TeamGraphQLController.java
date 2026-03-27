package com.ai.coach.controller;

import com.ai.coach.domain.entity.Player;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.service.PlayerService;
import com.ai.coach.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TeamGraphQLController {

    private final TeamService teamService;
    private final PlayerService playerService;

    @QueryMapping
    public List<Team> teams() {
        return teamService.getAllTeams();
    }

    @QueryMapping
    public Team team(@Argument Long id) {
        return teamService.getTeam(id);
    }

    @QueryMapping
    public List<Player> playersByTeam(@Argument Long teamId) {
        return playerService.getPlayersByTeam(teamId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Team createTeam(@Argument String name,
                           @Argument String league,
                           @Argument String formation) {
        return teamService.createTeam(name, league, formation);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Player createPlayer(@Argument Long teamId,
                               @Argument String name,
                               @Argument String position,
                               @Argument Double rating) {
        Team team = teamService.getTeam(teamId);
        if (team == null) {
            throw new EntityNotFoundException("Team", teamId);
        }
        Player player = Player.builder()
                .name(name)
                .position(position)
                .rating(rating)
                .team(team)
                .build();
        return playerService.createPlayer(player);
    }
}
