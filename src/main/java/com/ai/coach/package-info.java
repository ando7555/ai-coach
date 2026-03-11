/**
 * Root package of the AI Coach application.
 *
 * <p>AI Coach is a Spring Boot service that helps football coaches with
 * tactical analysis, training-plan generation, and season planning.
 * It combines a Neo4j graph database with Google Gemini AI to turn
 * raw match data into actionable coaching insights exposed through a
 * GraphQL API.</p>
 *
 * <h2>Package layout</h2>
 * <ul>
 *   <li>{@code config}       – framework and infrastructure configuration</li>
 *   <li>{@code controller}   – GraphQL query and mutation endpoints</li>
 *   <li>{@code domain}       – entities, DTOs, and repository interfaces</li>
 *   <li>{@code exception}    – custom exceptions and error handling</li>
 *   <li>{@code security}     – JWT authentication and authorization</li>
 *   <li>{@code service}      – business logic and AI integration</li>
 * </ul>
 *
 * @see com.ai.coach.config
 * @see com.ai.coach.controller
 * @see com.ai.coach.domain
 * @see com.ai.coach.exception
 * @see com.ai.coach.security
 * @see com.ai.coach.service
 */
package com.ai.coach;
