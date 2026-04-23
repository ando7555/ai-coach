package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.PlayerMatchStat;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PlayerMatchStatRepository extends Neo4jRepository<PlayerMatchStat, Long> {
    List<PlayerMatchStat> findByMatchId(Long matchId);
    List<PlayerMatchStat> findByPlayerId(Long playerId);
    List<PlayerMatchStat> findByPlayerIdAndMatchDateAfter(Long playerId, LocalDate after);
    List<PlayerMatchStat> findByPlayerIdInAndMatchDateAfter(Collection<Long> playerIds, LocalDate after);
}
