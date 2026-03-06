package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.TrainingPlan;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface TrainingPlanRepository extends Neo4jRepository<TrainingPlan, Long> {
    List<TrainingPlan> findByTeamId(Long teamId);
}