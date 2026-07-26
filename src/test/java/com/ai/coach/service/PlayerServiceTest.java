package com.ai.coach.service;

import com.ai.coach.domain.dto.CreatePlayerInput;
import com.ai.coach.domain.repository.PlayerRepository;
import com.ai.coach.domain.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {
    @Mock PlayerRepository playerRepository;
    @Mock TeamRepository teamRepository;

    @Test
    void rejectsBlankPlayerName() {
        PlayerService service = new PlayerService(playerRepository, teamRepository);

        assertThatThrownBy(() -> service.createPlayer(new CreatePlayerInput(1L, " ", "Forward", 7.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player name");
    }

    @Test
    void rejectsRatingOutsideTenPointScale() {
        PlayerService service = new PlayerService(playerRepository, teamRepository);

        assertThatThrownBy(() -> service.createPlayer(new CreatePlayerInput(1L, "Alex", "Forward", 10.5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 10");
    }
}
