package com.swp391.backend.service;

import com.swp391.backend.entity.IntegrationConfig;
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

    @Test
    void saveOrUpdate_NewValid_ShouldEncryptAndSave() {
        Long groupId = 1L;
        String repo = "owner/repo";
        String token = "raw-token";

        when(repository.findByGroupId(groupId)).thenReturn(Optional.empty());
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
    void saveOrUpdate_NewMissingToken_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> integrationService.saveOrUpdate(1L, "owner/repo", null));
        assertThrows(IllegalArgumentException.class, () -> integrationService.saveOrUpdate(1L, "owner/repo", "  "));
    }

    @Test
    void saveOrUpdate_InvalidRepoFormat_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> integrationService.saveOrUpdate(1L, "invalid-repo", "token"));
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

        when(repository.findByGroupId(groupId)).thenReturn(Optional.of(existing));
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

        when(repository.findByGroupId(groupId)).thenReturn(Optional.of(existing));
        when(repository.save(any(IntegrationConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        IntegrationConfig result = integrationService.saveOrUpdate(groupId, newRepo, null);

        assertEquals(newRepo, result.getRepoFullName());
        assertArrayEquals("old-encrypted".getBytes(), result.getTokenEncrypted());
        verify(tokenHelper, never()).encryptToBytes(anyString());
    }
}
