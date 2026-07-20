package com.ai.coach.predictor.feature;

import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.predictor.PredictorProperties;
import com.ai.coach.predictor.model.DataQualityStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchFeatureExtractorTest {
    @Test
    void excludesTargetMatchAndFutureMatchesFromSnapshot() {
        MatchRepository repository = mock(MatchRepository.class);
        PredictorProperties properties = new PredictorProperties();
        properties.setMinTeamMatches(1);
        properties.setMinGlobalMatches(2);
        MatchFeatureExtractor extractor = new MatchFeatureExtractor(repository, properties);

        Team home = Team.builder().id(1L).name("Home").build();
        Team away = Team.builder().id(2L).name("Away").build();
        Team other = Team.builder().id(3L).name("Other").build();
        Match target = match(10L, home, away, 9, 0, LocalDate.of(2026, 8, 10));
        Match homePast = match(1L, home, other, 2, 0, LocalDate.of(2026, 8, 1));
        Match awayPast = match(2L, other, away, 1, 2, LocalDate.of(2026, 8, 2));
        Match future = match(3L, home, away, 7, 7, LocalDate.of(2026, 8, 11));

        when(repository.findById(10L)).thenReturn(Optional.of(target));
        when(repository.findAll()).thenReturn(List.of(target, homePast, awayPast, future));

        MatchFeatureSnapshot snapshot = extractor.extract(10L);

        assertThat(snapshot.globalCompletedMatches()).isEqualTo(2);
        assertThat(snapshot.homeTeam().goalsForPerMatch()).isEqualTo(2.0);
        assertThat(snapshot.awayTeam().goalsForPerMatch()).isEqualTo(2.0);
        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.LIMITED);
    }

    private Match match(Long id, Team home, Team away, Integer homeGoals, Integer awayGoals, LocalDate date) {
        return Match.builder()
                .id(id)
                .homeTeam(home)
                .awayTeam(away)
                .homeGoals(homeGoals)
                .awayGoals(awayGoals)
                .date(date)
                .build();
    }
}
