package com.ai.coach.service;

import com.ai.coach.domain.dto.CreatePlayerInput;
import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.PlayerRepository;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    @Transactional
    public Player createPlayer(CreatePlayerInput input) {
        log.info("Creating player: {}", input.name());
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

        Player player = Player.builder()
                .name(input.name())
                .position(input.position())
                .rating(input.rating())
                .team(team)
                .build();

        return playerRepository.save(player);
    }
}
