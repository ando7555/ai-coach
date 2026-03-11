/**
 * Custom exceptions and GraphQL error handling for the AI Coach application.
 *
 * <p>This package centralises error semantics so that every failure —
 * whether from a missing entity, an AI generation timeout, or a validation
 * violation — is translated into a well-typed GraphQL error with the
 * correct classification (NOT_FOUND, BAD_REQUEST, UNAUTHORIZED, etc.).</p>
 *
 * <ul>
 *   <li>{@link com.ai.coach.exception.EntityNotFoundException} – thrown when
 *       a requested domain entity does not exist in Neo4j.</li>
 *   <li>{@link com.ai.coach.exception.AiGenerationException} – thrown when
 *       the AI model fails to produce a valid response.</li>
 *   <li>{@link com.ai.coach.exception.GraphQLExceptionHandler} – resolves
 *       all runtime exceptions into GraphQL-compliant errors.</li>
 * </ul>
 */
package com.ai.coach.exception;
