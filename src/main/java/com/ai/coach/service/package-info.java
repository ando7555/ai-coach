/**
 * Business logic and AI integration layer for the PitchMind application.
 *
 * <p>Services orchestrate domain operations, enforce business rules, and
 * delegate to the AI client for content generation. Each service is a
 * Spring-managed bean injected into the corresponding GraphQL controller.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.service.AuthService} - Google account
 *       authentication, admin allow-list enforcement, and JWT issuance.</li>
 *   <li>{@link com.ai.coach.service.GoogleIdentityService} - Google ID token
 *       validation against the configured OAuth client ID.</li>
 *   <li>{@link com.ai.coach.service.TeamService} - team CRUD operations.</li>
 *   <li>{@link com.ai.coach.service.MatchService} - match recording and
 *       retrieval.</li>
 *   <li>{@link com.ai.coach.service.PlayerMatchStatService} - player
 *       statistics tracking and performance-trend analysis.</li>
 *   <li>{@link com.ai.coach.service.RecommendationService} - AI-driven
 *       tactical recommendations for specific matches.</li>
 *   <li>{@link com.ai.coach.service.MatchAnalysisService} - AI-driven
 *       match analysis generation.</li>
 *   <li>{@link com.ai.coach.service.TrainingPlanService} - AI-driven
 *       training-plan generation.</li>
 *   <li>{@link com.ai.coach.service.SeasonPlanService} - AI-driven
 *       season-plan and workload snapshot generation.</li>
 *   <li>{@link com.ai.coach.service.AiClient} - thin wrapper around
 *       Spring AI that provides specialised chat clients with tailored
 *       system prompts.</li>
 * </ul>
 */
package com.ai.coach.service;
