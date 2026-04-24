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
        log.debug("Recording match: home={}, away={}", input.homeTeamId(), input.awayTeamId());
        Team home = teamRepository.findById(input.homeTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.homeTeamId()));
        Team away = teamRepository.findById(input.awayTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.awayTeamId()));

        LocalDate date = input.date() != null
                ? LocalDate.parse(input.date())
                : LocalDate.now();

        Match match = Match.builder()
                .homeTeam(home)
                .awayTeam(away)
                .homeGoals(input.homeGoals())
                .awayGoals(input.awayGoals())
                .date(date)
                .build();
        return matchRepository.save(match);
    }

}
