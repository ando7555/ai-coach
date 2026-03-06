package com.ai.coach.controller;


import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.PlayerRepository;
import com.ai.coach.service.TeamService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class TeamGraphQLController {

    private final TeamService teamService;
    private final PlayerRepository playerRepository;

    public TeamGraphQLController(TeamService teamService, PlayerRepository playerRepository) {
        this.teamService = teamService;
        this.playerRepository = playerRepository;
    }

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
        return playerRepository.findByTeamId(teamId);
    }

    @MutationMapping
    public Team createTeam(@Argument String name,
                           @Argument String league,
                           @Argument String formation) {
        return teamService.createTeam(name, league, formation);
    }

    @MutationMapping
    public Player createPlayer(@Argument Long teamId,
                               @Argument String name,
                               @Argument String position,
                               @Argument Double rating) {
        Team team = teamService.getTeam(teamId);
        if (team == null) {
            throw new IllegalArgumentException("Team not found");
        }
        Player player = new Player(name, position, rating);
        player.setTeam(team);
        return playerRepository.save(player);
    }
}
