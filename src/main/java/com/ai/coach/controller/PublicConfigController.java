package com.ai.coach.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicConfigController {

    private final String googleClientId;

    public PublicConfigController(@Value("${pitchmind.auth.google-client-id:}") String googleClientId) {
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
    }

    @GetMapping("/api/public-config")
    public PublicConfig publicConfig() {
        return new PublicConfig(googleClientId);
    }

    public record PublicConfig(String googleClientId) {}
}
