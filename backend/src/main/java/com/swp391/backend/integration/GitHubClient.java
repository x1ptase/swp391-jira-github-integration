package com.swp391.backend.integration;

import com.swp391.backend.dto.response.GitHubRepoResponse;
import com.swp391.backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final RestTemplate restTemplate;

    public GitHubRepoResponse getRepositoryInfo(String repoFullName, String token) {
        String url = "https://api.github.com/repos/" + repoFullName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GitHubRepoResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubRepoResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new BusinessException("Failed to fetch repository info from GitHub: " + response.getStatusCode(),
                        response.getStatusCode().value());
            }
        } catch (HttpClientErrorException e) {
            String message = e.getResponseBodyAsString();
            if (message == null || message.isEmpty()) {
                message = e.getStatusText();
            }
            throw new BusinessException("GitHub API Error: " + message, e.getStatusCode().value());
        } catch (Exception e) {
            throw new BusinessException("GitHub Connection Error: " + e.getMessage(), 500);
        }
    }
}
