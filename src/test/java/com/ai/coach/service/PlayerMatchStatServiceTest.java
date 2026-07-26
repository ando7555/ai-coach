package com.ai.coach.service;

import com.ai.coach.domain.dto.PlayerMatchStatInput;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerMatchStatServiceTest {
    @Mock PlayerMatchStatRepository statRepository;
    @Mock PlayerRepository playerRepository;
    @Mock MatchRepository matchRepository;

    @Test
    void rejectsStatsForScheduledFixture() {
        Team home = Team.builder().id(1L).name("Home").build();
        Team away = Team.builder().id(2L).name("Away").build();
        Player player = Player.builder().id(10L).name("Forward").position("Forward").team(home).build();
        Match match = Match.builder().id(20L).homeTeam(home).awayTeam(away).date(LocalDate.of(2026, 8, 1)).build();
        PlayerMatchStatService service = new PlayerMatchStatService(statRepository, playerRepository, matchRepository);

        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(matchRepository.findById(20L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.record(new PlayerMatchStatInput(10L, 20L, 90, 1, 0, 0, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completed match result");
    }

    @Test
    void rejectsPlayerGoalsAboveTeamScore() {
        Team home = Team.builder().id(1L).name("Home").build();
        Team away = Team.builder().id(2L).name("Away").build();
        Player player = Player.builder().id(10L).name("Forward").position("Forward").team(home).build();
        Match match = Match.builder().id(20L).homeTeam(home).awayTeam(away)
                .homeGoals(1).awayGoals(0).date(LocalDate.of(2026, 8, 1)).build();
        PlayerMatchStatService service = new PlayerMatchStatService(statRepository, playerRepository, matchRepository);

        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(matchRepository.findById(20L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.record(new PlayerMatchStatInput(10L, 20L, 90, 2, 0, 0, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }
}
