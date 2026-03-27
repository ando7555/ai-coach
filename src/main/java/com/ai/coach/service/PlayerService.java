package com.ai.coach.service;

import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    @Transactional
    public Player createPlayer(Player player) {
        return playerRepository.save(player);
    }
}
