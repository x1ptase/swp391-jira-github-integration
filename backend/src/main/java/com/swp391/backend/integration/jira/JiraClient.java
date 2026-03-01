package com.swp391.backend.integration.jira;

import com.swp391.backend.dto.response.JiraProjectResponse;
import com.swp391.backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class JiraClient {

    private final RestTemplate restTemplate;

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
        // Normalize baseUrl: bỏ dấu / ở cuối để tránh //rest/...
        String normalizedBaseUrl = baseUrl.stripTrailing().replaceAll("/+$", "");
        String url = normalizedBaseUrl + "/rest/api/3/project/" + projectKey;

        // Build Basic Auth header: Base64(email:token) – không log token
        String credentials = jiraEmail + ":" + token;
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

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
            // Bắt cả 4xx lẫn 5xx
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
}
