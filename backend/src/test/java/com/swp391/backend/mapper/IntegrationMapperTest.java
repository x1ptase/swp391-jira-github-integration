package com.swp391.backend.mapper;

import com.swp391.backend.dto.response.IntegrationResponseDTO;
import com.swp391.backend.entity.Integration;
import com.swp391.backend.service.TokenCryptoService;
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
}
