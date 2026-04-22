package com.ai.coach.service;

import com.ai.coach.domain.dto.*;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.PlayerMatchStat;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

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

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public PlayerMatchStatConnection getByPlayer(Long playerId, Integer first, String after) {
        List<PlayerMatchStat> allStats = statRepository.findByPlayerId(playerId);
        allStats = allStats.stream()
                .sorted(Comparator.comparing((PlayerMatchStat s) -> s.getMatch().getDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(PlayerMatchStat::getId, Comparator.reverseOrder()))
                .toList();

        int pageSize = first != null && first > 0 ? first : DEFAULT_PAGE_SIZE;
        Long afterId = decodeCursor(after);

        List<PlayerMatchStat> filtered = allStats;
        if (afterId != null) {
            int idx = -1;
            for (int i = 0; i < allStats.size(); i++) {
                if (allStats.get(i).getId().equals(afterId)) {
                    idx = i;
                    break;
                }
            }
            filtered = idx >= 0 && idx + 1 < allStats.size()
                    ? allStats.subList(idx + 1, allStats.size())
                    : List.of();
        }

        boolean hasNextPage = filtered.size() > pageSize;
        List<PlayerMatchStat> page = filtered.size() > pageSize ? filtered.subList(0, pageSize) : filtered;

        List<PlayerMatchStatEdge> edges = page.stream()
                .map(s -> new PlayerMatchStatEdge(s, encodeCursor(s.getId())))
                .toList();

        String endCursor = edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor();

        return new PlayerMatchStatConnection(edges, new PageInfo(hasNextPage, endCursor), allStats.size());
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

        FormIndicator form = calculateForm(stats);

        return new PlayerPerformanceTrend(
                player, matchCount, totalGoals, totalAssists, totalGoalContributions,
                avgGoals, avgAssists, avgContributions, avgRating,
                totalMinutes, form, stats
        );
    }

    private static final int RECENT_WINDOW = 5;
    private static final int PREVIOUS_WINDOW = 5;
    private static final double GOAL_WEIGHT = 3.0;
    private static final double ASSIST_WEIGHT = 2.0;
    private static final double RATING_WEIGHT = 0.1;
    private static final double FORM_CHANGE_THRESHOLD = 0.15;

    private FormIndicator calculateForm(List<PlayerMatchStat> stats) {
        if (stats.size() < 2) {
            return FormIndicator.STABLE;
        }
        int recentSize = Math.min(RECENT_WINDOW, stats.size() / 2);
        int previousSize = Math.min(PREVIOUS_WINDOW, stats.size() - recentSize);

        List<PlayerMatchStat> recentWindow = stats.subList(stats.size() - recentSize, stats.size());
        List<PlayerMatchStat> previousWindow = stats.subList(stats.size() - recentSize - previousSize, stats.size() - recentSize);

        double previousScore = averageCompositeScore(previousWindow);
        double recentScore = averageCompositeScore(recentWindow);

        if (previousScore == 0 && recentScore == 0) {
            return FormIndicator.STABLE;
        }
        if (previousScore == 0) {
            return FormIndicator.IMPROVING;
        }

        double changeRatio = (recentScore - previousScore) / previousScore;
        if (changeRatio > FORM_CHANGE_THRESHOLD) {
            return FormIndicator.IMPROVING;
        } else if (changeRatio < -FORM_CHANGE_THRESHOLD) {
            return FormIndicator.DECLINING;
        }
        return FormIndicator.STABLE;
    }

    private double averageCompositeScore(List<PlayerMatchStat> stats) {
        return stats.stream()
                .mapToDouble(s -> s.getGoals() * GOAL_WEIGHT
                        + s.getAssists() * ASSIST_WEIGHT
                        + (s.getRating() != null ? s.getRating() * RATING_WEIGHT : 0.0))
                .average()
                .orElse(0.0);
    }

    @Transactional
    public PlayerMatchStat record(PlayerMatchStatInput input) {
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

    static String encodeCursor(Long id) {
        return Base64.getEncoder().encodeToString(("cursor:" + id).getBytes());
    }

    static Long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String decoded = new String(Base64.getDecoder().decode(cursor));
        return Long.valueOf(decoded.substring("cursor:".length()));
    }
}
