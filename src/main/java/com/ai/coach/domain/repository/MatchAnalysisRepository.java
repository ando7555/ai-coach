package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.MatchAnalysis;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import java.util.List;

public interface MatchAnalysisRepository extends Neo4jRepository<MatchAnalysis, Long> {
    List<MatchAnalysis> findByMatchId(Long matchId);
}
