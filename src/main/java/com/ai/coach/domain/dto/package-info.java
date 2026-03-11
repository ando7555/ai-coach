/**
 * Data Transfer Objects for the AI Coach GraphQL API.
 *
 * <p>Input DTOs carry validated client data into service methods, while
 * output DTOs shape the responses returned to callers. Using DTOs keeps
 * the API contract independent of the Neo4j entity model.</p>
 *
 * <h2>Input DTOs</h2>
 * <ul>
 *   <li>{@link com.ai.coach.domain.dto.MatchInput}</li>
 *   <li>{@link com.ai.coach.domain.dto.MatchAnalysisInput}</li>
 *   <li>{@link com.ai.coach.domain.dto.TrainingPlanInput}</li>
 *   <li>{@link com.ai.coach.domain.dto.SeasonPlanInput}</li>
 *   <li>{@link com.ai.coach.domain.dto.PlayerMatchStatInput}</li>
 * </ul>
 *
 * <h2>Output DTOs</h2>
 * <ul>
 *   <li>{@link com.ai.coach.domain.dto.PlayerPerformanceTrend} – aggregated
 *       player statistics with a form indicator.</li>
 * </ul>
 */
package com.ai.coach.domain.dto;
