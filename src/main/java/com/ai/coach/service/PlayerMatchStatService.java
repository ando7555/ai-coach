package com.ai.coach.service;

import com.ai.coach.domain.dto.PlayerMatchStatInput;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.PlayerMatchStat;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerMatchStatService {

    private final PlayerMatchStatRepository statRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<PlayerMatchStat> getByMatch(Long matchId) {
        return statRepository.findByMatchId(matchId);
    }

    @Transactional(readOnly = true)
    public List<PlayerMatchStat> getByPlayer(Long playerId) {
        return statRepository.findByPlayerId(playerId);
    }

    @Transactional
    public PlayerMatchStat record(PlayerMatchStatInput input) {
        Player player = playerRepository.findById(input.playerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + input.playerId()));

        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + input.matchId()));

        PlayerMatchStat stat = PlayerMatchStat.builder()
                .player(player)
                .match(match)
                .minutesPlayed(input.minutesPlayed())
                .goals(input.goals() != null ? input.goals() : 0)
                .assists(input.assists() != null ? input.assists() : 0)
                .yellowCards(input.yellowCards() != null ? input.yellowCards() : 0)
                .redCard(input.redCard() != null && input.redCard())
                .build();

        return statRepository.save(stat);
    }
}
