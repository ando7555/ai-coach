package com.ai.coach.predictor;

import com.ai.coach.audit.PredictionAuditMapper;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.MatchPredictionRecord;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchPredictionRecordRepository;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.predictor.feature.MatchFeatureExtractor;
import com.ai.coach.predictor.feature.MatchFeatureSnapshot;
import com.ai.coach.predictor.feature.TeamFeatureSummary;
import com.ai.coach.predictor.model.ConfidenceLevel;
import com.ai.coach.predictor.model.DataQualityStatus;
import com.ai.coach.predictor.model.MatchPredictionPayload;
import com.ai.coach.predictor.model.MatchPredictionResult;
import com.ai.coach.predictor.model.UncertaintyLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchPredictionServiceTest {
    @Test
    void generatingAgainCreatesNextPredictionVersion() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchPredictionRecordRepository predictionRepository = mock(MatchPredictionRecordRepository.class);
        MatchFeatureExtractor extractor = mock(MatchFeatureExtractor.class);
        MatchPredictor predictor = mock(MatchPredictor.class);
        MatchPredictionService service = new MatchPredictionService(
                matchRepository, predictionRepository, extractor, predictor, new PredictionAuditMapper());

        Team home = Team.builder().id(1L).name("Home").build();
        Team away = Team.builder().id(2L).name("Away").build();
        Match match = Match.builder().id(5L).homeTeam(home).awayTeam(away).date(LocalDate.of(2026, 8, 1)).build();
        MatchFeatureSnapshot snapshot = snapshot();
        List<MatchPredictionRecord> savedRecords = new ArrayList<>();

        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(extractor.extract(match, List.of(match))).thenReturn(snapshot);
        when(predictor.predict(snapshot)).thenReturn(result());
        when(predictionRepository.findByMatchId(5L)).thenAnswer(invocation -> List.copyOf(savedRecords));
        when(predictionRepository.save(any(MatchPredictionRecord.class))).thenAnswer(invocation -> {
            MatchPredictionRecord record = invocation.getArgument(0);
            record.setId((long) savedRecords.size() + 1);
            savedRecords.add(record);
            return record;
        });

        MatchPredictionPayload first = service.generate(5L);
        MatchPredictionPayload second = service.generate(5L);

        assertThat(first.predictionVersion()).isEqualTo(1);
        assertThat(second.predictionVersion()).isEqualTo(2);
        assertThat(savedRecords).hasSize(2);
    }

    private MatchFeatureSnapshot snapshot() {
        TeamFeatureSummary home = new TeamFeatureSummary(1L, "Home", 4, 2, 2,
                1.5, 1.0, 1.6, 0.8, 1.4, 1.2, 1.5, 1.0);
        TeamFeatureSummary away = new TeamFeatureSummary(2L, "Away", 4, 2, 2,
                1.2, 1.3, 1.5, 1.0, 0.9, 1.6, 1.1, 1.4);
        return new MatchFeatureSnapshot(5L, 1L, 2L, "Home", "Away",
                LocalDate.of(2026, 8, 1), OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                home, away, 8, 1.4, 1.1, List.of("history"), List.of(), DataQualityStatus.SUFFICIENT);
    }

    private MatchPredictionResult result() {
        return new MatchPredictionResult(5L, 1L, 2L, 0.45, 0.25, 0.30,
                1.5, 1.1, 0.51, 0.49, 0.51, "1-1",
                ConfidenceLevel.MEDIUM, UncertaintyLevel.MEDIUM, DataQualityStatus.SUFFICIENT,
                List.of("factor"), List.of(), "model", "v1", OffsetDateTime.parse("2026-08-01T01:00:00Z"));
    }
}
