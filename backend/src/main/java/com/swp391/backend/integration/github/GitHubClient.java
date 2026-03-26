package com.swp391.backend.integration.github;

import com.swp391.backend.utils.DateTimeUtils;
import com.swp391.backend.dto.request.CommitSearchRequest;
import com.swp391.backend.dto.response.GitHubCommitDTO;
import com.swp391.backend.dto.response.GitHubCommitDetailedDTO;
import com.swp391.backend.dto.response.GitHubCommitResponse;
import com.swp391.backend.dto.response.GitHubRepoResponse;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final RestTemplate restTemplate;

    public GitHubRepoResponse getRepoInfo(String repoFullName, String token) {
        String url = "https://api.github.com/repos/" + repoFullName;

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(token));

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
            handleException(e);
            return null; // Should not reach here
        } catch (Exception e) {
            throw new BusinessException("GitHub Connection Error: " + e.getMessage(), 500);
        }
    }

    public List<GitHubCommitDTO> fetchAllCommits(String repoFullName, String token) {
        List<GitHubCommitDTO> allCommits = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            String url = String.format("https://api.github.com/repos/%s/commits?per_page=%d&page=%d",
                    repoFullName, perPage, page);

            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(token));

            try {
                ResponseEntity<List<GitHubCommitDTO>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<GitHubCommitDTO>>() {
                        });

                List<GitHubCommitDTO> commits = response.getBody();

                if (commits == null || commits.isEmpty()) {
                    break;
                }

                allCommits.addAll(commits);
                page++;
            } catch (HttpClientErrorException e) {
                handleException(e);
            } catch (Exception e) {
                throw new BusinessException("GitHub Connection Error: " + e.getMessage(), 500);
            }
        }

        return allCommits;
    }

    public GitHubCommitDetailedDTO fetchSingleCommit(String repoFullName, String sha, String token) {
        String url = String.format("https://api.github.com/repos/%s/commits/%s", repoFullName, sha);

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(token));

        try {
            ResponseEntity<GitHubCommitDetailedDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubCommitDetailedDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            handleException(e);
            return null;
        } catch (Exception e) {
            throw new BusinessException("GitHub Connection Error: " + e.getMessage(), 500);
        }
    }

    public List<GitHubCommitResponse> fetchCommitsWithCriteria(String repoFullName, String token,
            CommitSearchRequest criteria) {
        List<GitHubCommitResponse> allCommits = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString("https://api.github.com/repos/" + repoFullName + "/commits")
                    .queryParam("per_page", perPage)
                    .queryParam("page", page);

            // Xử lý query parameters dựa trên criteria
            if (criteria.getLastNDays() != null && criteria.getLastNDays() > 0) {
                builder.queryParam("since", DateTimeUtils.getIsoDateLastNDays(criteria.getLastNDays()));
            } else {
                if (criteria.getFromDate() != null && !criteria.getFromDate().isEmpty()) {
                    builder.queryParam("since", criteria.getFromDate());
                }
                if (criteria.getToDate() != null && !criteria.getToDate().isEmpty()) {
                    builder.queryParam("until", criteria.getToDate());
                }
            }

            String url = builder.build().toUriString();
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(token));

            try {
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });

                List<Map<String, Object>> commitMaps = response.getBody();

                if (commitMaps == null || commitMaps.isEmpty()) {
                    break;
                }

                for (Map<String, Object> map : commitMaps) {
                    allCommits.add(mapToGitHubCommitResponse(map));
                }

                page++;
            } catch (HttpClientErrorException e) {
                handleException(e);
            } catch (Exception e) {
                throw new BusinessException("GitHub Connection Error: " + e.getMessage(), 500);
            }
        }

        return allCommits;
    }

    @SuppressWarnings("unchecked")
    private GitHubCommitResponse mapToGitHubCommitResponse(Map<String, Object> map) {
        // commit và author có thể null nếu GitHub trả dữ liệu dị thường
        Map<String, Object> commit = map.get("commit") instanceof Map<?, ?>
                ? (Map<String, Object>) map.get("commit")
                : null;
        Map<String, Object> author = (commit != null && commit.get("author") instanceof Map<?, ?>)
                ? (Map<String, Object>) commit.get("author")
                : null;

        return GitHubCommitResponse.builder()
                .sha((String) map.get("sha"))
                .authorName(author != null ? (String) author.get("name") : null)
                .authorEmail(author != null ? (String) author.get("email") : null)
                .date(author != null ? (String) author.get("date") : null)
                .message(commit != null ? (String) commit.get("message") : null)
                .build();
    }

    private void handleException(HttpClientErrorException e) {
        HttpStatus status = (HttpStatus) e.getStatusCode();

        // Xử lý Rate Limit (403 hoặc 429)
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
            String rateLimitReset = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("x-ratelimit-reset")
                    : null;
            if (rateLimitReset != null) {
                try {
                    long resetTimestamp = Long.parseLong(rateLimitReset);
                    String formattedTime = Instant.ofEpochSecond(resetTimestamp)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    // Ném ngay, không fall-through
                    throw new GitHubApiException(
                            "GitHub Rate Limit exceeded. Resets at: " + formattedTime,
                            status.value());
                } catch (NumberFormatException nfe) {
                    // Header không phải số hợp lệ → fallthrough, throw generic message bên dưới
                    throw new GitHubApiException(
                            "GitHub Rate Limit exceeded (unknown reset time)",
                            status.value());
                }
            }
            // Không có header x-ratelimit-reset → thông báo chung
            throw new GitHubApiException("GitHub Rate Limit or Forbidden", status.value());
        }

        // Các lỗi HTTP khác (401 Unauthorized, 404 Not Found…)
        String message = e.getResponseBodyAsString();
        if (message == null || message.isEmpty()) {
            message = e.getStatusText();
        }
        throw new BusinessException("GitHub API Error: " + message, status.value());
    }

    private HttpHeaders buildHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.set(HttpHeaders.USER_AGENT, "SWP391-Project-Team");
        return headers;
    }
}
