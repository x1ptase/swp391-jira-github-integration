package com.swp391.backend.mapper;

import com.swp391.backend.dto.response.IntegrationResponse;
import com.swp391.backend.dto.response.IntegrationResponseDTO;
import com.swp391.backend.entity.Integration;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.service.TokenCryptoService;
import com.swp391.backend.service.TokenHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class IntegrationMapperTest {

    @Mock
    private TokenCryptoService tokenCryptoService;

    @Mock
    private TokenHelper tokenHelper;

    @InjectMocks
    private IntegrationMapper integrationMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void toResponseDTO_WithToken_ShouldMaskCorrectly() {
        Integration integration = Integration.builder()
                .id(1L)
                .name("Github Integration")
                .source("Github")
                .encryptedToken("encrypted_ghp_123456789")
                .build();

        when(tokenCryptoService.decrypt("encrypted_ghp_123456789")).thenReturn("ghp_123456789");

        IntegrationResponseDTO responseDTO = integrationMapper.toResponseDTO(integration);

        assertNotNull(responseDTO);
        assertEquals(1L, responseDTO.getId());
        assertEquals("Github Integration", responseDTO.getName());
        assertEquals("Github", responseDTO.getSource());
        assertTrue(responseDTO.isHasToken());
        assertEquals("****6789", responseDTO.getTokenMasked());
    }

    @Test
    void toResponseDTO_WithoutToken_ShouldSetHasTokenFalse() {
        Integration integration = Integration.builder()
                .id(2L)
                .name("Jira Integration")
                .source("Jira")
                .encryptedToken(null)
                .build();

        IntegrationResponseDTO responseDTO = integrationMapper.toResponseDTO(integration);

        assertNotNull(responseDTO);
        assertFalse(responseDTO.isHasToken());
        assertNull(responseDTO.getTokenMasked());
    }

    @Test
    void toResponseDTO_WithShortToken_ShouldMaskFully() {
        Integration integration = Integration.builder()
                .id(3L)
                .encryptedToken("short")
                .build();

        when(tokenCryptoService.decrypt("short")).thenReturn("abc");

        IntegrationResponseDTO responseDTO = integrationMapper.toResponseDTO(integration);

        assertEquals("****", responseDTO.getTokenMasked());
    }

    @Test
    void toResponse_ConfigWithToken_ShouldMaskCorrectly() {
        IntegrationConfig config = IntegrationConfig.builder()
                .id(1L)
                .repoFullName("owner/repo")
                .tokenEncrypted("encrypted_token")
                .build();

        when(tokenHelper.decrypt("encrypted_token")).thenReturn("ghp_123456789");
        when(tokenHelper.maskToken("ghp_123456789")).thenReturn("****6789");

        IntegrationResponse response = integrationMapper.toResponse(config);

        assertNotNull(response);
        assertEquals("owner/repo", response.getRepoFullName());
        assertTrue(response.isHasToken());
        assertEquals("****6789", response.getTokenMasked());
    }

    @Test
    void toResponse_ConfigWithoutToken_ShouldSetHasTokenFalse() {
        IntegrationConfig config = IntegrationConfig.builder()
                .id(2L)
                .repoFullName("owner/repo2")
                .tokenEncrypted(null)
                .build();

        IntegrationResponse response = integrationMapper.toResponse(config);

        assertNotNull(response);
        assertEquals("owner/repo2", response.getRepoFullName());
        assertFalse(response.isHasToken());
        assertNull(response.getTokenMasked());
    }
}
