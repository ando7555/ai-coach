package com.ai.coach.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubIssue(
        int number,
        String title,
        @JsonProperty("html_url") String htmlUrl
) {}
