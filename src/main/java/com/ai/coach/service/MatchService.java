package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchConnection;
import com.ai.coach.domain.dto.MatchEdge;
import com.ai.coach.domain.dto.MatchInput;
import com.ai.coach.domain.dto.PageInfo;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Match getMatch(Long id) {
        return matchRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public MatchConnection getMatchesByTeam(Long teamId, Integer first, String after) {
        List<Match> allMatches = matchRepository.findByHomeTeamIdOrAwayTeamId(teamId, teamId);
        allMatches = allMatches.stream()
                .sorted(Comparator.comparing(Match::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(Match::getId, Comparator.reverseOrder()))
                .toList();

        int pageSize = first != null && first > 0 ? first : DEFAULT_PAGE_SIZE;
        Long afterId = decodeCursor(after);

        List<Match> filtered = allMatches;
        if (afterId != null) {
            int idx = -1;
            for (int i = 0; i < allMatches.size(); i++) {
                if (allMatches.get(i).getId().equals(afterId)) {
                    idx = i;
                    break;
                }
            }
            filtered = idx >= 0 && idx + 1 < allMatches.size()
                    ? allMatches.subList(idx + 1, allMatches.size())
                    : List.of();
        }

        boolean hasNextPage = filtered.size() > pageSize;
        List<Match> page = filtered.size() > pageSize ? filtered.subList(0, pageSize) : filtered;

        List<MatchEdge> edges = page.stream()
                .map(m -> new MatchEdge(m, encodeCursor(m.getId())))
                .toList();

        String endCursor = edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor();

        return new MatchConnection(edges, new PageInfo(hasNextPage, endCursor), allMatches.size());
    }

    @Transactional
    public Match recordMatch(MatchInput input) {
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

    static String encodeCursor(Long id) {
        return Base64.getEncoder().encodeToString(("cursor:" + id).getBytes());
    }

    static Long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String decoded = new String(Base64.getDecoder().decode(cursor));
        return Long.valueOf(decoded.substring("cursor:".length()));
    }
}
