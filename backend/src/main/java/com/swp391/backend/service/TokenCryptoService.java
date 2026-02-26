package com.swp391.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting Jira / GitHub tokens using AES-256-CBC.
 *
 * <p>
 * The secret key is read from {@code app.crypto.secret-key} in
 * {@code application.properties}. It must be exactly 16, 24, or 32 characters
 * long (AES-128 / AES-192 / AES-256).
 *
 * <p>
 * Encrypted format stored in the DB: {@code Base64(IV + ciphertext)}
 * — the 16-byte IV is prepended so that every encryption call produces a
 * different result for the same plaintext.
 */
@Service
public class TokenCryptoService {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALG = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16; // bytes — always 16 for AES

    private final SecretKeySpec secretKey;

    public TokenCryptoService(
            @Value("${app.crypto.secret-key}") String rawKey) {

        byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "app.crypto.secret-key must be 16, 24, or 32 characters long " +
                            "(got " + keyBytes.length + " bytes).");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Encrypts {@code rawToken} and returns a Base64 string.
     * Format: {@code Base64(IV || ciphertext)}.
     *
     * @param rawToken the plaintext token to encrypt
     * @return Base64-encoded encrypted string
     * @throws RuntimeException wrapping any {@link GeneralSecurityException}
     */
    public String encrypt(String rawToken) {
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(CIPHER_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] cipherText = cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));

            // Prepend IV so we can recover it during decryption
            byte[] ivAndCipher = concat(iv, cipherText);
            return Base64.getEncoder().encodeToString(ivAndCipher);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Token encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64 string produced by {@link #encrypt(String)}.
     *
     * @param encryptedToken Base64-encoded encrypted string
     * @return original plaintext token
     * @throws RuntimeException wrapping any {@link GeneralSecurityException}
     */
    public String decrypt(String encryptedToken) {
        try {
            byte[] ivAndCipher = Base64.getDecoder().decode(encryptedToken);

            if (ivAndCipher.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted token is too short to contain a valid IV.");
            }

            byte[] iv = slice(ivAndCipher, 0, IV_LENGTH);
            byte[] cipherText = slice(ivAndCipher, IV_LENGTH, ivAndCipher.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALG);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Token decryption failed", e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static byte[] slice(byte[] src, int from, int to) {
        byte[] result = new byte[to - from];
        System.arraycopy(src, from, result, 0, result.length);
        return result;
    }
}
