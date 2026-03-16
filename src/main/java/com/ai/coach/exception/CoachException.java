package com.ai.coach.exception;

/**
 * Sealed base class for all domain-specific exceptions in AI Coach.
 * Only {@link EntityNotFoundException} and {@link AiGenerationException} are permitted subtypes.
 *
 * <p>Sealed classes (Java 17) restrict which classes can extend a type,
 * making the exception hierarchy explicit and compiler-enforced.</p>
 */
public sealed class CoachException extends RuntimeException
        permits EntityNotFoundException, AiGenerationException {

    protected CoachException(String message) {
        super(message);
    }

    protected CoachException(String message, Throwable cause) {
        super(message, cause);
    }
}
