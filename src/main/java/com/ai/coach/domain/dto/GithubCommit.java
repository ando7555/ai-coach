package com.ai.coach.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubCommit(
        String sha,
        String message,
        @JsonProperty("html_url") String htmlUrl
) {}
