package com.group21.marksystem.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for password hashing, including legacy and corrupt hashes.
 */
class PasswordUtilsTest {

    @Test
    void hashesAndVerifiesPasswords() {
        String hash = PasswordUtils.hashPassword("secret123");

        assertTrue(hash.startsWith("pbkdf2$"));
        assertTrue(PasswordUtils.verifyPassword("secret123", hash));
        assertFalse(PasswordUtils.verifyPassword("wrong", hash));
    }

    @Test
    void usesARandomSaltPerHash() {
        assertNotEquals(PasswordUtils.hashPassword("same"), PasswordUtils.hashPassword("same"));
    }

    @Test
    void stillAcceptsLegacySaltedSha256Hashes() throws Exception {
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) (i + 1);
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        byte[] hash = digest.digest("pass123".getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[48];
        System.arraycopy(salt, 0, combined, 0, 16);
        System.arraycopy(hash, 0, combined, 16, 32);
        String legacy = Base64.getEncoder().encodeToString(combined);

        assertTrue(PasswordUtils.verifyPassword("pass123", legacy));
        assertFalse(PasswordUtils.verifyPassword("wrong", legacy));
    }

    @Test
    void rejectsMalformedHashesWithoutThrowing() {
        String plaintext = Base64.getEncoder()
                .encodeToString("pass123".getBytes(StandardCharsets.UTF_8));

        assertFalse(PasswordUtils.verifyPassword("pass123", plaintext));
        assertFalse(PasswordUtils.verifyPassword("pass123", "AAAA"));
        assertFalse(PasswordUtils.verifyPassword("pass123", "not base64!!"));
        assertFalse(PasswordUtils.verifyPassword("pass123", "pbkdf2$abc$@@$@@"));
        assertFalse(PasswordUtils.verifyPassword("pass123", "pbkdf2$99999999$c2FsdA==$aGFzaA=="));
        assertFalse(PasswordUtils.verifyPassword("pass123", "pbkdf2$1000$c2FsdA=="));
    }

    @Test
    void nullInputsAreRejectedGracefully() {
        assertFalse(PasswordUtils.verifyPassword(null, "hash"));
        assertFalse(PasswordUtils.verifyPassword("password", null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtils.hashPassword(null));
    }
}
