/**
 * Spring Data Neo4j repository interfaces for the AI Coach domain.
 *
 * <p>Each repository extends {@code Neo4jRepository} and provides
 * graph-aware CRUD plus custom finder methods. Spring Data generates
 * the Cypher queries at runtime from the method signatures.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.domain.repository.UserRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.TeamRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.PlayerRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.MatchRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.PlayerMatchStatRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.MatchAnalysisRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.RecommendationRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.TrainingPlanRepository}</li>
 *   <li>{@link com.ai.coach.domain.repository.SeasonPlanRepository}</li>
 * </ul>
 */
package com.ai.coach.domain.repository;
