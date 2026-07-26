package com.ai.coach.predictor.evaluation;

import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.MatchPredictionRecord;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchPredictionRecordRepository;
import com.ai.coach.predictor.model.DataQualityStatus;
import com.ai.coach.predictor.model.PredictionEvaluationSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredictionEvaluationServiceTest {
    @Test
    void summarizesLatestCompletedPredictions() {
        MatchPredictionRecordRepository repository = mock(MatchPredictionRecordRepository.class);
        PredictionEvaluationService service = new PredictionEvaluationService(repository);
        Team home = Team.builder().id(1L).name("Home").build();
        Team away = Team.builder().id(2L).name("Away").build();
        Match completed = Match.builder().id(99L).homeTeam(home).awayTeam(away)
                .homeGoals(2).awayGoals(1).date(LocalDate.of(2026, 8, 1)).build();
        Match scheduled = Match.builder().id(100L).homeTeam(home).awayTeam(away)
                .date(LocalDate.of(2026, 8, 8)).build();

        when(repository.findAll()).thenReturn(List.of(
                prediction(completed, 0.20, 0.30, 0.50, "missing-time", null),
                prediction(completed, 0.30, 0.40, 0.30, "old", OffsetDateTime.parse("2026-07-31T10:00:00Z")),
                prediction(completed, 0.55, 0.25, 0.20, "new", OffsetDateTime.parse("2026-07-31T11:00:00Z")),
                prediction(scheduled, 0.55, 0.25, 0.20, "new", OffsetDateTime.parse("2026-07-31T12:00:00Z"))
        ));

        PredictionEvaluationSummary summary = service.summarizeLatestCompletedPredictions();

        assertThat(summary.generatedPredictions()).isEqualTo(4);
        assertThat(summary.evaluatedPredictions()).isEqualTo(1);
        assertThat(summary.correctWinnerPredictions()).isEqualTo(1);
        assertThat(summary.winnerAccuracy()).isEqualTo(1.0);
        assertThat(summary.averageBrierScore()).isNotNull();
        assertThat(summary.modelVersion()).isEqualTo("new");
    }

    private MatchPredictionRecord prediction(Match match, double home, double draw, double away,
                                             String version, OffsetDateTime generatedAt) {
        return MatchPredictionRecord.builder()
                .match(match)
                .homeWinProbability(home)
                .drawProbability(draw)
                .awayWinProbability(away)
                .dataQualityStatus(DataQualityStatus.SUFFICIENT)
                .modelVersion(version)
                .generatedAt(generatedAt)
                .build();
    }
}
