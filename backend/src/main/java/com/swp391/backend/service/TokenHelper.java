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
public class TokenHelper {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALG = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private final SecretKeySpec secretKey;

    public TokenHelper(@Value("${app.crypto.secret-key}") String rawKey) {
        byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "app.security.aes-key must be 16, 24, or 32 characters long.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public String encrypt(String rawToken) {
        if (rawToken == null)
            return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] cipherText = cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            byte[] ivAndCipher = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, ivAndCipher, IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(ivAndCipher);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedToken) {
        if (encryptedToken == null)
            return null;
        try {
            byte[] ivAndCipher = Base64.getDecoder().decode(encryptedToken);
            if (ivAndCipher.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted token");
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(ivAndCipher, 0, iv, 0, IV_LENGTH);

            byte[] cipherText = new byte[ivAndCipher.length - IV_LENGTH];
            System.arraycopy(ivAndCipher, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALG);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String maskToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return "";
        }
        if (rawToken.length() <= 4) {
            return "****";
        }
        return "****" + rawToken.substring(rawToken.length() - 4);
    }
}
