/**
 * Framework and infrastructure configuration for the AI Coach application.
 *
 * <p>This package contains Spring {@code @Configuration} classes that wire up
 * cross-cutting concerns such as database connectivity and security.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.config.Neo4jConfig} – enables Neo4j auditing and
 *       configures the graph database connection.</li>
 *   <li>{@link com.ai.coach.config.SecurityConfig} – sets up Spring Security
 *       with JWT-based stateless authentication, CSRF disabled, and
 *       role-based access control.</li>
 * </ul>
 */
package com.ai.coach.config;
