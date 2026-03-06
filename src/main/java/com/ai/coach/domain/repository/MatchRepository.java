package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.Match;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface MatchRepository extends Neo4jRepository<Match, Long> {
    List<Match> findByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);
}
