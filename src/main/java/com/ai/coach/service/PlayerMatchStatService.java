package com.ai.coach.service;

import com.ai.coach.domain.CursorPaginator;
import com.ai.coach.domain.FormCalculator;
import com.ai.coach.domain.dto.*;
import com.ai.coach.domain.entity.FormIndicator;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.PlayerMatchStat;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
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
public class PlayerMatchStatService {

    private final PlayerMatchStatRepository statRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<PlayerMatchStat> getByMatch(Long matchId) {
        return statRepository.findByMatchId(matchId);
    }

    @Transactional(readOnly = true)
    public PlayerMatchStatConnection getByPlayer(Long playerId, Integer first, String after) {
        List<PlayerMatchStat> allStats = statRepository.findByPlayerId(playerId);
        allStats = allStats.stream()
                .sorted(Comparator.comparing((PlayerMatchStat s) -> s.getMatch().getDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(PlayerMatchStat::getId, Comparator.reverseOrder()))
                .toList();

        CursorPaginator.Page<PlayerMatchStat> page =
                CursorPaginator.paginate(allStats, PlayerMatchStat::getId, first, after);

        List<PlayerMatchStatEdge> edges = page.items().stream()
                .map(s -> new PlayerMatchStatEdge(s, CursorPaginator.encodeCursor(s.getId())))
                .toList();

        return new PlayerMatchStatConnection(edges, page.pageInfo(), page.totalCount());
    }

    @Transactional(readOnly = true)
    public PlayerPerformanceTrend getTrendByLastMatches(Long playerId, int lastN) {
        if (lastN < 1) {
            throw new IllegalArgumentException("lastN must be at least 1");
        }
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player", playerId));

        List<PlayerMatchStat> allStats = statRepository.findByPlayerId(playerId);
        List<PlayerMatchStat> recent = allStats.stream()
                .sorted(Comparator.comparing(s -> s.getMatch().getDate()))
                .skip(Math.max(0, allStats.size() - lastN))
                .toList();

        return buildTrend(player, recent);
    }

    @Transactional(readOnly = true)
    public PlayerPerformanceTrend getTrendByDateRange(Long playerId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player", playerId));

        List<PlayerMatchStat> allStats = statRepository.findByPlayerId(playerId);
        List<PlayerMatchStat> stats = allStats.stream()
                .filter(s -> {
                    LocalDate matchDate = s.getMatch().getDate();
                    return matchDate != null && !matchDate.isBefore(from) && !matchDate.isAfter(to);
                })
                .sorted(Comparator.comparing(s -> s.getMatch().getDate()))
                .toList();

        return buildTrend(player, stats);
    }

    private record StatAccumulator(int goals, int assists, int minutes, double ratingSum, int ratedCount) {
        static final StatAccumulator IDENTITY = new StatAccumulator(0, 0, 0, 0.0, 0);

        StatAccumulator add(PlayerMatchStat s) {
            double r = s.getRating() != null ? s.getRating() : 0.0;
            int rc = s.getRating() != null ? 1 : 0;
            return new StatAccumulator(goals + s.getGoals(), assists + s.getAssists(),
                    minutes + s.getMinutesPlayed(), ratingSum + r, ratedCount + rc);
        }

        StatAccumulator merge(StatAccumulator other) {
            return new StatAccumulator(goals + other.goals, assists + other.assists,
                    minutes + other.minutes, ratingSum + other.ratingSum, ratedCount + other.ratedCount);
        }
    }

    private PlayerPerformanceTrend buildTrend(Player player, List<PlayerMatchStat> stats) {
        int matchCount = stats.size();

        StatAccumulator acc = stats.stream().reduce(
                StatAccumulator.IDENTITY, StatAccumulator::add, StatAccumulator::merge);

        int totalGoals = acc.goals();
        int totalAssists = acc.assists();
        int totalGoalContributions = totalGoals + totalAssists;
        int totalMinutes = acc.minutes();

        double avgGoals = matchCount > 0 ? (double) totalGoals / matchCount : 0.0;
        double avgAssists = matchCount > 0 ? (double) totalAssists / matchCount : 0.0;
        double avgContributions = matchCount > 0 ? (double) totalGoalContributions / matchCount : 0.0;
        Double avgRating = acc.ratedCount() > 0 ? acc.ratingSum() / acc.ratedCount() : null;

        FormIndicator form = FormCalculator.calculateForm(stats);

        return new PlayerPerformanceTrend(
                player, matchCount, totalGoals, totalAssists, totalGoalContributions,
                avgGoals, avgAssists, avgContributions, avgRating,
                totalMinutes, form, stats
        );
    }

    @Transactional
    public PlayerMatchStat record(PlayerMatchStatInput input) {
        log.debug("Recording stat: player={}, match={}", input.playerId(), input.matchId());
        Player player = playerRepository.findById(input.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player", input.playerId()));

        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

        PlayerMatchStat stat = PlayerMatchStat.builder()
                .player(player)
                .match(match)
                .minutesPlayed(input.minutesPlayed())
                .goals(input.goals() != null ? input.goals() : 0)
                .assists(input.assists() != null ? input.assists() : 0)
                .yellowCards(input.yellowCards() != null ? input.yellowCards() : 0)
                .redCard(input.redCard() != null && input.redCard())
                .build();

        return statRepository.save(stat);
    }

}
