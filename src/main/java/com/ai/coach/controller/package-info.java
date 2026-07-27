/**
 * GraphQL query and mutation controllers for the PitchMind API.
 *
 * <p>Every public endpoint in the application is a GraphQL operation defined
 * in this package. Controllers delegate to the {@code service} layer and are
 * secured via {@code @PreAuthorize} annotations where needed.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.controller.AuthGraphQLController} - Google account
 *       authentication mutation.</li>
 *   <li>{@link com.ai.coach.controller.TeamGraphQLController} - team and
 *       player queries and mutations.</li>
 *   <li>{@link com.ai.coach.controller.MatchGraphQLController} - match
 *       recording and retrieval.</li>
 *   <li>{@link com.ai.coach.controller.PlayerMatchStatController} - player
 *       statistics and performance-trend queries.</li>
 *   <li>{@link com.ai.coach.controller.RecommendationGraphQLController} -
 *       AI-generated tactical recommendations.</li>
 *   <li>{@link com.ai.coach.controller.MatchAnalysisGraphQLController} -
 *       match analysis AI features.</li>
 *   <li>{@link com.ai.coach.controller.TrainingPlanGraphQLController} -
 *       training-plan AI features.</li>
 *   <li>{@link com.ai.coach.controller.SeasonPlanGraphQLController} -
 *       season-plan AI features.</li>
 * </ul>
 */
package com.ai.coach.controller;
