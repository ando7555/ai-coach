/**
 * Root package of the PitchMind application.
 *
 * <p>PitchMind is a Spring Boot service for football intelligence,
 * tactical analysis, workload planning, and future transparent prediction
 * and betting decision support. Current production code focuses on
 * coaching workflows; prediction and betting-market evaluation are
 * documented as roadmap capabilities until implemented in dedicated
 * services.</p>
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
