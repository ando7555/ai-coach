package com.ai.coach.exception;

public final class AiGenerationException extends CoachException {

    private final String operation;

    public AiGenerationException(String operation, Throwable cause) {
        super("AI generation failed for: %s".formatted(operation), cause);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
