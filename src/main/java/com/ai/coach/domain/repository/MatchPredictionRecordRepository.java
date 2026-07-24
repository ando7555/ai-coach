package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.MatchPredictionRecord;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;
import java.util.Optional;

public interface MatchPredictionRecordRepository extends Neo4jRepository<MatchPredictionRecord, Long> {
    List<MatchPredictionRecord> findByMatchId(Long matchId);

    Optional<MatchPredictionRecord> findFirstByMatchIdOrderByGeneratedAtDesc(Long matchId);
}
