/**
 * Neo4j node entities that model the football coaching domain.
 *
 * <p>Each class in this package maps to a node (or relationship) in the
 * Neo4j graph database. Relationships such as {@code HAS_PLAYER},
 * {@code PLAYS_FOR}, and {@code ANALYSIS_FOR} capture the natural
 * connections in a football domain.</p>
 *
 * <h2>Core entities</h2>
 * <ul>
 *   <li>{@link com.ai.coach.domain.entity.User} – application user
 *       with role-based access (COACH / ADMIN).</li>
 *   <li>{@link com.ai.coach.domain.entity.Team} – football team with
 *       formation and league metadata.</li>
 *   <li>{@link com.ai.coach.domain.entity.Player} – player linked to
 *       a team via {@code PLAYS_FOR}.</li>
 *   <li>{@link com.ai.coach.domain.entity.Match} – recorded match with
 *       home/away teams and score.</li>
 * </ul>
 *
 * <h2>AI-generated entities</h2>
 * <ul>
 *   <li>{@link com.ai.coach.domain.entity.MatchAnalysis} – tactical
 *       analysis produced by the coaching AI.</li>
 *   <li>{@link com.ai.coach.domain.entity.TrainingPlan} – weekly
 *       micro-cycled training plan with sessions.</li>
 *   <li>{@link com.ai.coach.domain.entity.SeasonPlan} – long-term
 *       season plan with workload snapshots.</li>
 *   <li>{@link com.ai.coach.domain.entity.Recommendation} – in-context
 *       tactical advice for a specific match.</li>
 * </ul>
 */
package com.ai.coach.domain.entity;
