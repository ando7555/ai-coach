package com.ai.coach.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionGraphQLControllerTest {
    @Test
    void predictionOperationsRequireAuthentication() {
        Arrays.stream(PredictionGraphQLController.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .forEach(method -> assertThat(method.getAnnotation(PreAuthorize.class))
                        .as(method.getName())
                        .isNotNull());
    }
}
