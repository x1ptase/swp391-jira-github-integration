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

    public String decrypt(String encryptedToken) {
        try {
            byte[] ivAndCipher = Base64.getDecoder().decode(encryptedToken);
            return decryptFromBytes(ivAndCipher);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 format for token", e);
        }
    }

    public String decryptFromBytes(byte[] ivAndCipher) {
        try {
            if (ivAndCipher == null) {
                return null;
            }

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
