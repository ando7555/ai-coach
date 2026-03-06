package com.ai.coach.exception;

public class AiGenerationException extends RuntimeException {

    private final String operation;

    public AiGenerationException(String operation, Throwable cause) {
        super("AI generation failed for: %s".formatted(operation), cause);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
