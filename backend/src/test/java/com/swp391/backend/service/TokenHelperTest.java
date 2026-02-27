package com.swp391.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHelperTest {

    private TokenHelper tokenHelper;
    private final String aesKey = "1234567890123456"; // 16 bytes for AES-128

    @BeforeEach
    void setUp() {
        tokenHelper = new TokenHelper(aesKey);
    }

    @Test
    void encryptAndDecrypt_ShouldReturnOriginalToken() {
        String originalToken = "my-secret-github-token";
        String encrypted = tokenHelper.encrypt(originalToken);

        assertNotNull(encrypted);
        assertNotEquals(originalToken, encrypted);

        String decrypted = tokenHelper.decrypt(encrypted);
        assertEquals(originalToken, decrypted);
    }

    @Test
    void maskToken_ShouldMaskCorrectly() {
        assertEquals("****5678", tokenHelper.maskToken("12345678"));
        assertEquals("****", tokenHelper.maskToken("123"));
        assertEquals("", tokenHelper.maskToken(null));
        assertEquals("", tokenHelper.maskToken(""));
        assertEquals("****abcd", tokenHelper.maskToken("ghp_abcd"));
    }

    @Test
    void encrypt_WithNull_ShouldReturnNull() {
        assertNull(tokenHelper.encrypt(null));
    }

    @Test
    void decrypt_WithNull_ShouldReturnNull() {
        assertNull(tokenHelper.decrypt(null));
    }
}
