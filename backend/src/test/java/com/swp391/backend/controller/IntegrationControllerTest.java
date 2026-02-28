package com.swp391.backend.controller;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.request.GitHubConfigRequest;
import com.swp391.backend.dto.request.JiraConfigRequest;
import com.swp391.backend.dto.response.IntegrationResponse;
import com.swp391.backend.dto.response.JiraIntegrationResponse;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.entity.User;
import com.swp391.backend.mapper.IntegrationMapper;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.GroupService;
import com.swp391.backend.service.IntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationControllerTest {

    @Mock
    private IntegrationService integrationService;
    @Mock
    private IntegrationMapper integrationMapper;
    @Mock
    private IntegrationConfigRepository repository;
    @Mock
    private GroupService groupService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IntegrationController integrationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("testuser");

        User user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    // ── GitHub config ─────────────────────────────────────────────────────────

    @Test
    void saveGitHubConfig_Authorized_ShouldReturnResponse() {
        Long groupId = 100L;
        GitHubConfigRequest request = new GitHubConfigRequest("owner/repo", "token");

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(true);
        when(integrationService.saveOrUpdate(anyLong(), anyString(), anyString()))
                .thenReturn(new IntegrationConfig());
        when(integrationMapper.toResponse(any())).thenReturn(new IntegrationResponse());

        ResponseEntity<IntegrationResponse> result = integrationController.saveGitHubConfig(groupId, request);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void saveGitHubConfig_Unauthorized_ShouldThrowAccessDenied() {
        Long groupId = 100L;
        GitHubConfigRequest request = new GitHubConfigRequest("owner/repo", "token");

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> integrationController.saveGitHubConfig(groupId, request));
    }

    @Test
    void getGitHubConfig_Authorized_ShouldReturnConfig() {
        Long groupId = 100L;
        IntegrationConfig config = new IntegrationConfig();

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(true);
        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB))
                .thenReturn(Optional.of(config));
        when(integrationMapper.toResponse(config)).thenReturn(new IntegrationResponse());

        ResponseEntity<IntegrationResponse> result = integrationController.getGitHubConfig(groupId);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void getGitHubConfig_NotFound_ShouldReturn404() {
        Long groupId = 100L;

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(true);
        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB))
                .thenReturn(Optional.empty());

        ResponseEntity<IntegrationResponse> result = integrationController.getGitHubConfig(groupId);

        assertEquals(404, result.getStatusCode().value());
    }

    // ── Jira config ───────────────────────────────────────────────────────────

    @Test
    void saveJiraConfig_Authorized_ShouldReturnResponse() {
        Long groupId = 100L;
        JiraConfigRequest request = new JiraConfigRequest(
                "https://org.atlassian.net", "SWP391", "leader@gmail.com", "token");

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(true);
        when(integrationService.saveOrUpdateJira(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new IntegrationConfig());
        when(integrationMapper.toJiraResponse(any())).thenReturn(new JiraIntegrationResponse());

        ResponseEntity<JiraIntegrationResponse> result = integrationController.saveJiraConfig(groupId, request);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void saveJiraConfig_Unauthorized_ShouldThrowAccessDenied() {
        Long groupId = 100L;
        JiraConfigRequest request = new JiraConfigRequest(
                "https://org.atlassian.net", "SWP391", "leader@gmail.com", "token");

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> integrationController.saveJiraConfig(groupId, request));
    }

    @Test
    void getJiraConfig_Authorized_ShouldReturnConfig() {
        Long groupId = 100L;
        IntegrationConfig config = new IntegrationConfig();

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(true);
        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA))
                .thenReturn(Optional.of(config));
        when(integrationMapper.toJiraResponse(config)).thenReturn(new JiraIntegrationResponse());

        ResponseEntity<JiraIntegrationResponse> result = integrationController.getJiraConfig(groupId);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void getJiraConfig_NotFound_ShouldReturn404() {
        Long groupId = 100L;

        when(groupService.isUserAuthorized(eq(1L), eq(groupId), anyList())).thenReturn(true);
        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA))
                .thenReturn(Optional.empty());

        ResponseEntity<JiraIntegrationResponse> result = integrationController.getJiraConfig(groupId);

        assertEquals(404, result.getStatusCode().value());
    }
}
