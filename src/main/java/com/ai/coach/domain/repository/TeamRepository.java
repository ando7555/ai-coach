package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.Team;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface TeamRepository extends Neo4jRepository<Team, Long> {
}
