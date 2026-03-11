/**
 * Business logic and AI integration layer for the AI Coach application.
 *
 * <p>Services orchestrate domain operations, enforce business rules, and
 * delegate to the AI client for content generation. Each service is a
 * Spring-managed bean injected into the corresponding GraphQL controller.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.service.AuthService} – user registration and
 *       login with password hashing and JWT issuance.</li>
 *   <li>{@link com.ai.coach.service.TeamService} – team CRUD operations.</li>
 *   <li>{@link com.ai.coach.service.MatchService} – match recording and
 *       retrieval.</li>
 *   <li>{@link com.ai.coach.service.PlayerMatchStatService} – player
 *       statistics tracking and performance-trend analysis.</li>
 *   <li>{@link com.ai.coach.service.RecommendationService} – AI-driven
 *       tactical recommendations for specific matches.</li>
 *   <li>{@link com.ai.coach.service.CoachService} – core coaching AI
 *       features: match analysis, training-plan, and season-plan
 *       generation.</li>
 *   <li>{@link com.ai.coach.service.AiClient} – thin wrapper around
 *       Spring AI that provides three specialised chat clients (tactical,
 *       training, season) with tailored system prompts.</li>
 * </ul>
 */
package com.ai.coach.service;
