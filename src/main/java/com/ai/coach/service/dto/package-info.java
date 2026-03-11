/**
 * Service-layer Data Transfer Objects used to parse AI model responses.
 *
 * <p>These records map the structured JSON returned by Google Gemini into
 * typed Java objects so that the service layer can persist them as Neo4j
 * entities.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.service.dto.AiTrainingPlanResponse} – captures
 *       the summary and individual session entries of an AI-generated
 *       training plan.</li>
 *   <li>{@link com.ai.coach.service.dto.AiSeasonPlanResponse} – captures
 *       the summary and strategic objectives of an AI-generated season
 *       plan.</li>
 * </ul>
 */
package com.ai.coach.service.dto;
