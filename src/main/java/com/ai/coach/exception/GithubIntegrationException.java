package com.ai.coach.exception;

public final class GithubIntegrationException extends CoachException {

    public GithubIntegrationException(String message) {
        super(message);
    }

    public GithubIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
