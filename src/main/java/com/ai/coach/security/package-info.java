/**
 * JWT-based authentication and authorization for the AI Coach API.
 *
 * <p>This package implements stateless security using JSON Web Tokens.
 * On every request the {@link com.ai.coach.security.JwtAuthenticationFilter}
 * extracts the Bearer token from the {@code Authorization} header, validates
 * it via {@link com.ai.coach.security.JwtTokenProvider}, and populates the
 * Spring Security context with the authenticated principal and its
 * granted authorities.</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.security.JwtTokenProvider} – generates and
 *       validates HMAC-SHA signed JWT tokens with configurable expiry.</li>
 *   <li>{@link com.ai.coach.security.JwtAuthenticationFilter} – servlet
 *       filter that intercepts requests and sets up the security context.</li>
 * </ul>
 */
package com.ai.coach.security;
