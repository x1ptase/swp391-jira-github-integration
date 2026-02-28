package com.swp391.backend.service;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.impl.IntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class IntegrationServiceTest {

    @Mock
    private IntegrationConfigRepository repository;

    @Mock
    private TokenHelper tokenHelper;

    @InjectMocks
    private IntegrationServiceImpl integrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── GitHub: saveOrUpdate ──────────────────────────────────────────────────

    @Test
    void saveOrUpdate_NewValid_ShouldEncryptAndSave() {
        Long groupId = 1L;
        String repo = "owner/repo";
        String token = "raw-token";

        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB))
                .thenReturn(Optional.empty());
        when(tokenHelper.encryptToBytes(token)).thenReturn("encrypted-token".getBytes());
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdate(groupId, repo, token);

        assertNotNull(result);
        assertEquals(groupId, result.getGroupId());
        assertEquals(repo, result.getRepoFullName());
        assertArrayEquals("encrypted-token".getBytes(), result.getTokenEncrypted());
        verify(repository).save(any(IntegrationConfig.class));
    }

    @Test
    void saveOrUpdate_NewMissingToken_ShouldThrow400() {
        when(repository.findByGroupIdAndIntegrationTypeId(anyLong(), eq(IntegrationTypeIds.GITHUB)))
                .thenReturn(Optional.empty());

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> integrationService.saveOrUpdate(1L, "owner/repo", null));
        assertEquals(400, ex1.getStatus());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> integrationService.saveOrUpdate(1L, "owner/repo", "  "));
        assertEquals(400, ex2.getStatus());
    }

    @Test
    void saveOrUpdate_InvalidRepoFormat_ShouldThrow400() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> integrationService.saveOrUpdate(1L, "invalid-repo", "token"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void saveOrUpdate_UpdateWithToken_ShouldUpdateRepoAndToken() {
        Long groupId = 1L;
        String newRepo = "new/repo";
        String newToken = "new-token";
        IntegrationConfig existing = IntegrationConfig.builder()
                .groupId(groupId)
                .repoFullName("old/repo")
                .tokenEncrypted("old-encrypted".getBytes())
                .build();

        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB))
                .thenReturn(Optional.of(existing));
        when(tokenHelper.encryptToBytes(newToken)).thenReturn("new-encrypted".getBytes());
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdate(groupId, newRepo, newToken);

        assertEquals(newRepo, result.getRepoFullName());
        assertArrayEquals("new-encrypted".getBytes(), result.getTokenEncrypted());
        verify(tokenHelper).encryptToBytes(newToken);
    }

    @Test
    void saveOrUpdate_UpdateWithoutToken_ShouldKeepOldToken() {
        Long groupId = 1L;
        String newRepo = "new/repo";
        IntegrationConfig existing = IntegrationConfig.builder()
                .groupId(groupId)
                .repoFullName("old/repo")
                .tokenEncrypted("old-encrypted".getBytes())
                .build();

        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdate(groupId, newRepo, null);

        assertEquals(newRepo, result.getRepoFullName());
        assertArrayEquals("old-encrypted".getBytes(), result.getTokenEncrypted());
        verify(tokenHelper, never()).encryptToBytes(anyString());
    }

    // ── Jira: saveOrUpdateJira ────────────────────────────────────────────────

    @Test
    void saveOrUpdateJira_NewValid_ShouldEncryptAndSave() {
        Long groupId = 2L;
        String baseUrl = "https://myorg.atlassian.net";
        String projectKey = "SWP391";
        String jiraEmail = "leader@gmail.com";
        String token = "ATATTxxxxxxx";

        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA))
                .thenReturn(Optional.empty());
        when(tokenHelper.encryptToBytes(token)).thenReturn("jira-encrypted".getBytes());
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdateJira(groupId, baseUrl, projectKey, jiraEmail, token);

        assertNotNull(result);
        assertEquals(groupId, result.getGroupId());
        assertEquals(IntegrationTypeIds.JIRA, result.getIntegrationTypeId());
        assertEquals(baseUrl, result.getBaseUrl());
        assertEquals(projectKey, result.getProjectKey());
        assertEquals(jiraEmail, result.getJiraEmail());
        assertArrayEquals("jira-encrypted".getBytes(), result.getTokenEncrypted());
    }

    @Test
    void saveOrUpdateJira_NewMissingToken_ShouldThrow400() {
        when(repository.findByGroupIdAndIntegrationTypeId(anyLong(), eq(IntegrationTypeIds.JIRA)))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> integrationService.saveOrUpdateJira(2L,
                        "https://org.atlassian.net", "PROJ", "leader@gmail.com", null));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void saveOrUpdateJira_InvalidBaseUrl_ShouldThrow400() {
        when(repository.findByGroupIdAndIntegrationTypeId(anyLong(), eq(IntegrationTypeIds.JIRA)))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> integrationService.saveOrUpdateJira(2L,
                        "not-a-url", "PROJ", "leader@gmail.com", "token"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void saveOrUpdateJira_UpdateKeepsOldTokenWhenNotProvided() {
        Long groupId = 2L;
        IntegrationConfig existing = IntegrationConfig.builder()
                .groupId(groupId)
                .integrationTypeId(IntegrationTypeIds.JIRA)
                .baseUrl("https://old.atlassian.net")
                .projectKey("OLD")
                .jiraEmail("old@gmail.com")
                .tokenEncrypted("old-jira-encrypted".getBytes())
                .build();

        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdateJira(
                groupId, "https://new.atlassian.net", "NEW", "new@gmail.com", null);

        assertEquals("https://new.atlassian.net", result.getBaseUrl());
        assertEquals("NEW", result.getProjectKey());
        assertArrayEquals("old-jira-encrypted".getBytes(), result.getTokenEncrypted());
        verify(tokenHelper, never()).encryptToBytes(anyString());
    }

    @Test
    void saveOrUpdateJira_UpdateWithNewToken_ShouldReEncrypt() {
        Long groupId = 2L;
        IntegrationConfig existing = IntegrationConfig.builder()
                .groupId(groupId)
                .integrationTypeId(IntegrationTypeIds.JIRA)
                .baseUrl("https://old.atlassian.net")
                .projectKey("OLD")
                .jiraEmail("old@gmail.com")
                .tokenEncrypted("old-jira-encrypted".getBytes())
                .build();

        when(repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA))
                .thenReturn(Optional.of(existing));
        when(tokenHelper.encryptToBytes("new-token")).thenReturn("new-jira-encrypted".getBytes());
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdateJira(
                groupId, "https://new.atlassian.net", "NEW", "new@gmail.com", "new-token");

        assertArrayEquals("new-jira-encrypted".getBytes(), result.getTokenEncrypted());
        verify(tokenHelper).encryptToBytes("new-token");
    }
}
