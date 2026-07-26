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
        if (input == null) {
            throw new IllegalArgumentException("Player input is required");
        }
        Long teamId = requireId(input.teamId(), "Team id");
        String name = requireText(input.name(), "Player name");
        String position = requireText(input.position(), "Player position");
        Double rating = normalizeRating(input.rating());

        log.info("Creating player: {}", name);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team", teamId));

        Player player = Player.builder()
                .name(name)
                .position(position)
                .rating(rating)
                .team(team)
                .build();

        return playerRepository.save(player);
    }

    private Long requireId(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    private Double normalizeRating(Double rating) {
        if (rating == null) {
            return null;
        }
        if (rating < 0 || rating > 10) {
            throw new IllegalArgumentException("Player rating must be between 0 and 10");
        }
        return rating;
    }
}
