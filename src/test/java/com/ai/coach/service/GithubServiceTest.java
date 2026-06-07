package com.ai.coach.service;

import com.ai.coach.domain.dto.GithubCommit;
import com.ai.coach.domain.dto.GithubIssue;
import com.ai.coach.exception.GithubIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GithubService githubService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        githubService = new GithubService(webClientBuilder, "https://api.github.com", "test-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCommitFile_Success() throws Exception {
        // Mock GET contents returns existing SHA
        ObjectNode getResponse = objectMapper.createObjectNode();
        getResponse.put("sha", "existing-sha-123");

        // Mock PUT contents returns commit details
        ObjectNode putResponse = objectMapper.createObjectNode();
        ObjectNode commitNode = putResponse.putObject("commit");
        commitNode.put("sha", "new-commit-sha");
        commitNode.put("html_url", "https://github.com/test/html");

        // Set up WebClient GET mock chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/repos/{owner}/{repo}/contents/{path}"), eq("owner"), eq("repo"), eq("path.md")))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(getResponse));

        // Set up WebClient PUT mock chain
        when(webClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/repos/{owner}/{repo}/contents/{path}"), eq("owner"), eq("repo"), eq("path.md")))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any(Consumer.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(putResponse));

        // Execute
        GithubCommit commit = githubService.commitFile("owner", "repo", "path.md", "content", "msg");

        // Verify
        assertNotNull(commit);
        assertEquals("new-commit-sha", commit.sha());
        assertEquals("https://github.com/test/html", commit.htmlUrl());
        verify(webClient, times(1)).get();
        verify(webClient, times(1)).put();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCommitFile_NotFoundExceptionPropagatesCorrectly() {
        // Mock GET contents returns 404 (Not Found)
        WebClientResponseException notFoundEx = WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null);

        // Mock PUT contents returns commit details
        ObjectNode putResponse = objectMapper.createObjectNode();
        ObjectNode commitNode = putResponse.putObject("commit");
        commitNode.put("sha", "new-commit-sha");
        commitNode.put("html_url", "https://github.com/test/html");

        // Set up WebClient GET mock chain returning WebClientResponseException
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/repos/{owner}/{repo}/contents/{path}"), eq("owner"), eq("repo"), eq("path.md")))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.error(notFoundEx));

        // Set up WebClient PUT mock chain
        when(webClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/repos/{owner}/{repo}/contents/{path}"), eq("owner"), eq("repo"), eq("path.md")))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any(Consumer.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(putResponse));

        // Execute
        GithubCommit commit = githubService.commitFile("owner", "repo", "path.md", "content", "msg");

        // Verify
        assertNotNull(commit);
        assertEquals("new-commit-sha", commit.sha());
        verify(webClient, times(1)).get();
        verify(webClient, times(1)).put();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateIssue_Success() {
        GithubIssue expectedIssue = new GithubIssue(42, "Squad Alert", "https://github.com/test/issue/42");

        // Set up WebClient POST mock chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/repos/{owner}/{repo}/issues"), eq("owner"), eq("repo")))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any(Consumer.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubIssue.class)).thenReturn(Mono.just(expectedIssue));

        // Execute
        GithubIssue issue = githubService.createIssue("owner", "repo", "Squad Alert", "body");

        // Verify
        assertNotNull(issue);
        assertEquals(42, issue.number());
        assertEquals("Squad Alert", issue.title());
        assertEquals("https://github.com/test/issue/42", issue.htmlUrl());
        verify(webClient, times(1)).post();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateIssue_FailureThrowsException() {
        // Set up WebClient POST mock chain throwing error
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/repos/{owner}/{repo}/issues"), eq("owner"), eq("repo")))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any(Consumer.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubIssue.class)).thenReturn(Mono.error(new RuntimeException("API error")));

        // Execute & Verify
        assertThrows(GithubIntegrationException.class, () ->
                githubService.createIssue("owner", "repo", "Squad Alert", "body")
        );
    }
}
