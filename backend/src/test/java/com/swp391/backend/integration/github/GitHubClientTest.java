package com.swp391.backend.integration.github;

import com.swp391.backend.dto.request.CommitSearchRequest;
import com.swp391.backend.dto.response.GitHubCommitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubClientTest {

    private GitHubClient gitHubClient;
    private MockRestServiceServer server;
    private RestTemplate restTemplate;

    private final String repoFullName = "owner/repo";
    private final String token = "test-token";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        gitHubClient = new GitHubClient(restTemplate);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void fetchCommitsWithCriteria_WithLastNDays_ShouldBuildCorrectUrl() {
        CommitSearchRequest criteria = new CommitSearchRequest(null, null, 7);

        // Mock response
        String jsonResponse = "[{" +
                "\"sha\": \"sha123\"," +
                "\"commit\": {" +
                "  \"author\": {\"name\": \"Author Name\", \"email\": \"author@example.com\", \"date\": \"2024-03-05T00:00:00Z\"},"
                +
                "  \"message\": \"test message\"" +
                "}" +
                "}]";

        server.expect(
                requestTo(org.hamcrest.Matchers.containsString("https://api.github.com/repos/owner/repo/commits")))
                .andExpect(queryParam("per_page", "100"))
                .andExpect(queryParam("since", org.hamcrest.Matchers.notNullValue()))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Thoát vòng lặp while
        server.expect(requestTo(org.hamcrest.Matchers.containsString("page=2")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<GitHubCommitResponse> result = gitHubClient.fetchCommitsWithCriteria(repoFullName, token, criteria);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sha123", result.get(0).getSha());
        assertEquals("Author Name", result.get(0).getAuthorName());
        server.verify();
    }

    @Test
    void fetchCommitsWithCriteria_WithDateRange_ShouldBuildCorrectUrl() {
        CommitSearchRequest criteria = new CommitSearchRequest("2024-01-01T00:00:00Z", "2024-01-31T23:59:59Z", null);

        server.expect(
                requestTo(org.hamcrest.Matchers.containsString("https://api.github.com/repos/owner/repo/commits")))
                .andExpect(queryParam("since", "2024-01-01T00:00:00Z"))
                .andExpect(queryParam("until", "2024-01-31T23:59:59Z"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        gitHubClient.fetchCommitsWithCriteria(repoFullName, token, criteria);

        server.verify();
    }
}
