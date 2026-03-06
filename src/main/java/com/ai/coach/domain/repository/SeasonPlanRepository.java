package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.SeasonPlan;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface SeasonPlanRepository extends Neo4jRepository<SeasonPlan, Long> {
    List<SeasonPlan> findByTeamId(Long teamId);
}