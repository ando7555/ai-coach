package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchInput;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock MatchRepository matchRepository;
    @Mock TeamRepository teamRepository;

    @Test
    void rejectsSameHomeAndAwayTeam() {
        MatchService service = new MatchService(matchRepository, teamRepository);
        assertThatThrownBy(() -> service.recordMatch(new MatchInput(1L, 1L, 0, 0, "2026-07-05")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    void rejectsMalformedDateWithActionableMessage() {
        MatchService service = new MatchService(matchRepository, teamRepository);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(Team.builder().id(1L).name("Home").build()));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(Team.builder().id(2L).name("Away").build()));

        assertThatThrownBy(() -> service.recordMatch(new MatchInput(1L, 2L, 1, 0, "05/07/2026")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YYYY-MM-DD");
    }
}
