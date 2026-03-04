package com.swp391.backend.integration.jira;

import com.swp391.backend.dto.response.JiraProjectResponse;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.dto.JiraBulkFetchResponse;
import com.swp391.backend.integration.jira.dto.JiraSearchJqlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JiraClient {

    private final RestTemplate restTemplate;

    private static final List<String> BULK_FIELDS = List.of(
            "summary", "description", "issuetype", "status", "priority", "assignee", "updated");

    /**
     * Gọi Jira REST API v3 để lấy thông tin project.
     * Token KHÔNG được log hoặc đưa vào exception message.
     *
     * @param baseUrl    Jira base URL (ví dụ: https://myorg.atlassian.net)
     * @param projectKey Jira project key (ví dụ: SWP391)
     * @param jiraEmail  Email dùng để xác thực Jira
     * @param token      Jira API token (raw, không log)
     * @return JiraProjectResponse chứa key và name của project
     */
    public JiraProjectResponse getProjectInfo(String baseUrl, String projectKey,
            String jiraEmail, String token) {
        String normalizedBaseUrl = baseUrl.stripTrailing().replaceAll("/+$", "");
        String url = normalizedBaseUrl + "/rest/api/3/project/" + projectKey;

        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders(jiraEmail, token));

        try {
            ResponseEntity<JiraProjectResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JiraProjectResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Jira API returned unexpected status: " + response.getStatusCode().value(),
                        response.getStatusCode().value());
            }

        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            String message = switch (status) {
                case 401 -> "Jira authentication failed (invalid email/token)";
                case 403 -> "Jira access denied (no permission to access project)";
                case 404 -> "Jira project not found or no access to projectKey=" + projectKey;
                default -> "Jira API Error: " + e.getStatusCode().value() + " " + e.getStatusText();
            };
            throw new BusinessException(message, status);

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            throw new BusinessException("Jira Connection Error: " + e.getMessage(), 500);
        }
    }

    /**
     * Tìm issue IDs theo JQL dùng endpoint mới /rest/api/3/search/jql (Jira Cloud).
     * <p>
     * Endpoint /rest/api/3/search đã bị remove (410) trên Jira Cloud mới.
     * Token KHÔNG được log hoặc đưa vào exception message.
     *
     * @param baseUrl       Jira base URL
     * @param jiraEmail     Email xác thực
     * @param token         Jira API token (raw, không log)
     * @param jql           JQL query (đã escape/quote)
     * @param maxResults    Số lượng tối đa mỗi trang (1..100)
     * @param nextPageToken Opaque token từ page trước (null cho page đầu)
     * @return JiraSearchJqlResponse chứa issues[{id}], nextPageToken, isLast
     */
    public JiraSearchJqlResponse searchIssueIdsByJql(String baseUrl, String jiraEmail, String token,
            String jql, int maxResults,
            String nextPageToken) {
        String normalizedBaseUrl = baseUrl.stripTrailing().replaceAll("/+$", "");

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(normalizedBaseUrl + "/rest/api/3/search/jql")
                .queryParam("jql", jql)
                .queryParam("maxResults", maxResults);

        if (nextPageToken != null && !nextPageToken.isBlank()) {
            builder.queryParam("nextPageToken", nextPageToken);
        }

// IMPORTANT: dùng URI thay vì String + bỏ encode()
        java.net.URI uri = builder.build().toUri();

        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders(jiraEmail, token));



        try {
            System.out.println("JIRA_SEARCH_JQL_URL=" + uri);
            System.out.println("JIRA_SEARCH_JQL=" + jql);
            ResponseEntity<JiraSearchJqlResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    JiraSearchJqlResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Jira Search/JQL API returned unexpected status: " + response.getStatusCode().value(),
                        response.getStatusCode().value());
            }

        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            String message = switch (status) {
                case 400 -> "Invalid JQL query or request: " + sanitizeBody(e.getResponseBodyAsString());
                case 401 -> "Jira authentication failed (invalid email/token)";
                case 403 -> "Jira access denied (no permission to search issues)";
                case 404 -> "Jira Search/JQL endpoint not found";
                case 410 -> "Jira Search API has been removed; ensure you are using /rest/api/3/search/jql";
                default -> "Jira Search/JQL API Error: " + status + " " + e.getStatusText();
            };
            throw new BusinessException(message, status);

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            throw new BusinessException("Jira Search/JQL Connection Error: " + e.getMessage(), 500);
        }
    }

    /**
     * Lấy chi tiết nhiều issues cùng lúc qua POST /rest/api/3/issue/bulkfetch.
     * Token KHÔNG được log hoặc đưa vào exception message.
     *
     * @param baseUrl   Jira base URL
     * @param jiraEmail Email xác thực
     * @param token     Jira API token (raw, không log)
     * @param issueIds  Danh sách issue IDs cần lấy chi tiết
     * @return JiraBulkFetchResponse chứa issues đầy đủ fields
     */
    public JiraBulkFetchResponse bulkFetchIssueDetails(String baseUrl, String jiraEmail, String token,
            List<String> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return new JiraBulkFetchResponse();
        }

        String normalizedBaseUrl = baseUrl.stripTrailing().replaceAll("/+$", "");
        String url = normalizedBaseUrl + "/rest/api/3/issue/bulkfetch";

        Map<String, Object> requestBody = Map.of(
                "issueIdsOrKeys", issueIds,
                "fields", BULK_FIELDS);

        HttpHeaders headers = buildAuthHeaders(jiraEmail, token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<JiraBulkFetchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    JiraBulkFetchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Jira BulkFetch API returned unexpected status: " + response.getStatusCode().value(),
                        response.getStatusCode().value());
            }

        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            String message = switch (status) {
                case 400 -> "Jira BulkFetch: bad request - " + sanitizeBody(e.getResponseBodyAsString());
                case 401 -> "Jira authentication failed (invalid email/token)";
                case 403 -> "Jira access denied (no permission to bulk-fetch issues)";
                case 404 -> "Jira BulkFetch endpoint not found";
                default -> "Jira BulkFetch API Error: " + status + " " + e.getStatusText();
            };
            throw new BusinessException(message, status);

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            throw new BusinessException("Jira BulkFetch Connection Error: " + e.getMessage(), 500);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds Basic Auth headers using Base64(email:token).
     * Token is NEVER logged.
     */
    private HttpHeaders buildAuthHeaders(String jiraEmail, String token) {
        String credentials = jiraEmail + ":" + token;
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);
        return headers;
    }

    /**
     * Strips potential sensitive data from Jira error response bodies.
     * Truncates to 300 chars to avoid huge error messages.
     */
    private String sanitizeBody(String body) {
        if (body == null)
            return "";
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }
}
