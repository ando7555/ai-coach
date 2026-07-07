package com.ai.coach.service;

import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.FocusArea;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.entity.TrainingIntensity;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.domain.repository.TrainingPlanRepository;
import com.ai.coach.exception.AiGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {
    @Mock TeamRepository teamRepository;
    @Mock TrainingPlanRepository trainingPlanRepository;
    @Mock AiClient aiClient;
    @Mock AiResponseParser aiResponseParser;

    private TrainingPlanService service;

    @BeforeEach
    void setUp() {
        service = new TrainingPlanService(teamRepository, trainingPlanRepository, aiClient, aiResponseParser);
    }

    @Test
    void usesDeterministicFallbackWhenAiProviderFails() {
        Team team = Team.builder().id(1L).name("Valid FC").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(aiClient.generateTrainingPlan(any())).thenReturn(Mono.error(
                new AiGenerationException("Training plan", new RuntimeException("provider unavailable"))));
        when(trainingPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TrainingPlan result = service.generateTrainingPlan(new TrainingPlanInput(
                1L, "2026-07-06", "2026-07-12", FocusArea.BUILD_UP, TrainingIntensity.MEDIUM));

        assertThat(result.getSummary()).contains("Provider-independent");
        assertThat(result.getSessions()).hasSize(6);
        assertThat(result.getSessions()).allSatisfy(session -> {
            assertThat(session.getDate().toLocalDate()).isBetween(
                    result.getWeekStart().toLocalDate(), result.getWeekEnd().toLocalDate());
            assertThat(session.getDurationMinutes()).isPositive();
        });
    }

    @Test
    void rejectsReversedDateRangeBeforeCallingAi() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(Team.builder().id(1L).name("Valid FC").build()));

        assertThatThrownBy(() -> service.generateTrainingPlan(new TrainingPlanInput(
                1L, "2026-07-12", "2026-07-06", FocusArea.PRESSING, TrainingIntensity.HIGH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weekEnd");

        verify(aiClient, never()).generateTrainingPlan(any());
    }

    @Test
    void rejectsRangesLongerThanOneWeek() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(Team.builder().id(1L).name("Valid FC").build()));

        assertThatThrownBy(() -> service.generateTrainingPlan(new TrainingPlanInput(
                1L, "2026-07-01", "2026-07-15", FocusArea.DEFENCE, TrainingIntensity.LOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 days");
    }
}
