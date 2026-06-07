package com.ai.coach.service;

import com.ai.coach.domain.dto.GithubCommit;
import com.ai.coach.domain.dto.GithubIssue;
import com.ai.coach.exception.GithubIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GithubService {

    private final WebClient webClient;
    private final String token;

    public GithubService(
            WebClient.Builder webClientBuilder,
            @Value("${github.api-url}") String apiUrl,
            @Value("${github.token}") String token
    ) {
        this.token = token;
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "AI-Coach-Backend")
                .build();
    }

    /**
     * Commits a file (create or update) to the GitHub repository.
     */
    public GithubCommit commitFile(String owner, String repo, String path, String content, String commitMessage) {
        log.info("Committing file to GitHub: {}/{} path={}", owner, repo, path);

        // 1. Check if file exists to get existing SHA
        String existingSha = getFileSha(owner, repo, path);

        // 2. Prepare payload
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> body = new HashMap<>();
        body.put("message", commitMessage != null && !commitMessage.isBlank() ? commitMessage : "Update from AI Coach");
        body.put("content", base64Content);
        if (existingSha != null) {
            body.put("sha", existingSha);
        }

        try {
            JsonNode response = webClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .headers(headers -> {
                        if (token != null && !token.isBlank()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("commit")) {
                throw new GithubIntegrationException("Failed to parse commit response from GitHub");
            }

            JsonNode commitNode = response.get("commit");
            String sha = commitNode.get("sha").asText();
            String htmlUrl = commitNode.get("html_url").asText();

            return new GithubCommit(sha, commitMessage, htmlUrl);

        } catch (Exception e) {
            log.error("Failed to commit file to GitHub", e);
            throw new GithubIntegrationException("GitHub commit failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an issue in the GitHub repository.
     */
    public GithubIssue createIssue(String owner, String repo, String title, String bodyText) {
        log.info("Creating issue in GitHub: {}/{} title={}", owner, repo, title);

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("body", bodyText);

        try {
            return webClient.post()
                    .uri("/repos/{owner}/{repo}/issues", owner, repo)
                    .headers(headers -> {
                        if (token != null && !token.isBlank()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(GithubIssue.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to create issue in GitHub", e);
            throw new GithubIntegrationException("GitHub issue creation failed: " + e.getMessage(), e);
        }
    }

    private String getFileSha(String owner, String repo, String path) {
        try {
            JsonNode response = webClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .headers(headers -> {
                        if (token != null && !token.isBlank()) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("sha")) {
                return response.get("sha").asText();
            }
        } catch (WebClientResponseException.NotFound ex) {
            log.debug("File not found on GitHub contents GET (404). Assuming new file creation.");
        } catch (Exception e) {
            log.warn("Failed to check if file exists on GitHub: {}", e.getMessage());
        }
        return null;
    }
}
