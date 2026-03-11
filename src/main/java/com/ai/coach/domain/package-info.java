/**
 * Domain layer of the AI Coach application.
 *
 * <p>This package groups the core domain model into three sub-packages:</p>
 *
 * <ul>
 *   <li>{@code entity}     – Neo4j node entities and relationship mappings
 *       that represent the football domain (teams, players, matches,
 *       analyses, training plans, and season plans).</li>
 *   <li>{@code dto}        – Data Transfer Objects used as GraphQL inputs
 *       and outputs, keeping the API contract decoupled from persistence.</li>
 *   <li>{@code repository} – Spring Data Neo4j repository interfaces that
 *       provide graph-aware CRUD and custom queries.</li>
 * </ul>
 *
 * @see com.ai.coach.domain.entity
 * @see com.ai.coach.domain.dto
 * @see com.ai.coach.domain.repository
 */
package com.ai.coach.domain;
