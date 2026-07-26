package com.ai.coach.service;

import com.ai.coach.domain.CursorPaginator;
import com.ai.coach.domain.dto.MatchConnection;
import com.ai.coach.domain.dto.MatchEdge;
import com.ai.coach.domain.dto.MatchInput;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Match getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match", id));
    }

    @Transactional(readOnly = true)
    public MatchConnection getMatchesByTeam(Long teamId, Integer first, String after) {
        List<Match> allMatches = matchRepository.findByHomeTeamIdOrAwayTeamId(teamId, teamId);
        allMatches = allMatches.stream()
                .sorted(Comparator.comparing(Match::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(Match::getId, Comparator.reverseOrder()))
                .toList();

        CursorPaginator.Page<Match> page = CursorPaginator.paginate(allMatches, Match::getId, first, after);

        List<MatchEdge> edges = page.items().stream()
                .map(m -> new MatchEdge(m, CursorPaginator.encodeCursor(m.getId())))
                .toList();

        return new MatchConnection(edges, page.pageInfo(), page.totalCount());
    }

    @Transactional
    public Match recordMatch(MatchInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Match input is required");
        }
        Long homeTeamId = requireId(input.homeTeamId(), "Home team id");
        Long awayTeamId = requireId(input.awayTeamId(), "Away team id");
        validateScore(input.homeGoals(), input.awayGoals());

        log.debug("Recording match: home={}, away={}", homeTeamId, awayTeamId);
        if (homeTeamId.equals(awayTeamId)) {
            throw new IllegalArgumentException("Home and away teams must be different");
        }
        Team home = teamRepository.findById(homeTeamId)
                .orElseThrow(() -> new EntityNotFoundException("Team", homeTeamId));
        Team away = teamRepository.findById(awayTeamId)
                .orElseThrow(() -> new EntityNotFoundException("Team", awayTeamId));

        LocalDate date;
        try {
            if (input.date() == null || input.date().isBlank()) {
                throw new IllegalArgumentException("Match date is required");
            }
            date = LocalDate.parse(input.date());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Match date must use ISO format YYYY-MM-DD");
        }

        Match match = Match.builder()
                .homeTeam(home)
                .awayTeam(away)
                .homeGoals(input.homeGoals())
                .awayGoals(input.awayGoals())
                .date(date)
                .build();
        return matchRepository.save(match);
    }

    private Long requireId(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private void validateScore(Integer homeGoals, Integer awayGoals) {
        if ((homeGoals == null) != (awayGoals == null)) {
            throw new IllegalArgumentException("Both goal values must be provided for completed matches, or both left empty for scheduled fixtures");
        }
        if (homeGoals != null && (homeGoals < 0 || awayGoals < 0)) {
            throw new IllegalArgumentException("Match goals must be non-negative");
        }
    }
}
