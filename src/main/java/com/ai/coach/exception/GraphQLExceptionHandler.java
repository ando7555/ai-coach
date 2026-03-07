package com.ai.coach.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof EntityNotFoundException e) {
            return buildError(e.getMessage(), ErrorType.NOT_FOUND, env,
                    Map.of("entityType", e.getEntityType(), "entityId", String.valueOf(e.getEntityId())));
        }

        if (ex instanceof ConstraintViolationException e) {
            String message = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return buildError(message, ErrorType.BAD_REQUEST, env, Map.of());
        }

        if (ex instanceof IllegalArgumentException e) {
            return buildError(e.getMessage(), ErrorType.BAD_REQUEST, env, Map.of());
        }

        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            return buildError("Authentication required", ErrorType.UNAUTHORIZED, env, Map.of());
        }

        if (ex instanceof AiGenerationException e) {
            log.error("AI generation error: {}", e.getMessage(), e);
            return buildError(e.getMessage(), ErrorType.INTERNAL_ERROR, env,
                    Map.of("operation", e.getOperation()));
        }

        log.error("Unexpected error in GraphQL resolver", ex);
        return buildError("Internal server error", ErrorType.INTERNAL_ERROR, env, Map.of());
    }

    private GraphQLError buildError(String message, ErrorClassification classification,
                                     DataFetchingEnvironment env, Map<String, Object> extra) {
        var extensions = new java.util.HashMap<>(extra);
        extensions.put("classification", classification.toString());

        return GraphQLError.newError()
                .message(message)
                .errorType(classification)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .extensions(extensions)
                .build();
    }
}
