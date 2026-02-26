package com.swp391.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenCryptoServiceTest {

    private TokenCryptoService tokenCryptoService;
    private final String secretKey = "MySecretAESKey32CharactersLong!!";

    @BeforeEach
    void setUp() {
        tokenCryptoService = new TokenCryptoService(secretKey);
    }

    @Test
    void testEncryptDecrypt_Success() {
        String originalToken = "github_pat_1234567890abcdefghijklmnopqrstuvwxyz";

        String encrypted = tokenCryptoService.encrypt(originalToken);
        assertNotNull(encrypted);
        assertNotEquals(originalToken, encrypted);

        String decrypted = tokenCryptoService.decrypt(encrypted);
        assertEquals(originalToken, decrypted);
    }

    @Test
    void testEncrypt_ProducesDifferentResultsForSameInput() {
        String originalToken = "raw-token";

        String encrypted1 = tokenCryptoService.encrypt(originalToken);
        String encrypted2 = tokenCryptoService.encrypt(originalToken);

        // Due to random IV, every encryption should be unique
        assertNotEquals(encrypted1, encrypted2);

        // But both must decrypt to the same value
        assertEquals(originalToken, tokenCryptoService.decrypt(encrypted1));
        assertEquals(originalToken, tokenCryptoService.decrypt(encrypted2));
    }

    @Test
    void testDecrypt_InvalidInput_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            tokenCryptoService.decrypt("invalid-base64-string");
        });
    }

    @Test
    void testConstructor_InvalidKeyLength_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenCryptoService("short-key");
        });
    }
}
