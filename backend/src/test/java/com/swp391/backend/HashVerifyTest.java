package com.swp391.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashVerifyTest {
    @Test
    void testHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String generatedHash = encoder.encode("password123");
        System.out.println("GENERATED_HASH_START[" + generatedHash + "]GENERATED_HASH_END");
        assertTrue(encoder.matches("password123", generatedHash));
    }
}
