// RecommendationRepository.java
package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.Recommendation;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface RecommendationRepository extends Neo4jRepository<Recommendation, Long> {
    List<Recommendation> findByMatchId(Long matchId);
}
