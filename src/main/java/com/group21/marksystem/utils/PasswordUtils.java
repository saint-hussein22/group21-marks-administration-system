package com.group21.marksystem.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification.
 *
 * <p>New passwords are hashed with PBKDF2-HMAC-SHA256 and a random per-password
 * salt. Hashes created by older releases (salted SHA-256, Base64 encoded) are
 * still accepted so existing databases keep working.</p>
 *
 * @author Group 21
 */
public class PasswordUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int MAX_ITERATIONS = 1_000_000;

    /** Length of a legacy (salted SHA-256) hash: 16-byte salt + 32-byte digest. */
    private static final int LEGACY_LENGTH = 48;

    private PasswordUtils() {
        // Utility class
    }

    /**
     * Hashes a password using PBKDF2-HMAC-SHA256 with a random salt.
     *
     * @param password The plain text password to hash.
     * @return A self-describing string: {@code pbkdf2$iterations$salt$hash}.
     */
    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(password, salt, ITERATIONS);

        return PBKDF2_PREFIX
                + "$" + ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a password against a stored hash. Supports both PBKDF2 and the
     * legacy salted SHA-256 format.
     *
     * @param password   The plain text password to verify.
     * @param storedHash The stored hash.
     * @return true if the password matches the stored hash; false for a wrong
     *         password or an unreadable/corrupt hash.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        try {
            if (storedHash.startsWith(PBKDF2_PREFIX + "$")) {
                return verifyPbkdf2(password, storedHash);
            }
            return verifyLegacy(password, storedHash);
        } catch (RuntimeException | GeneralSecurityException e) {
            // Malformed Base64, wrong length, bad iteration count, missing
            // algorithm: all mean "this stored value cannot authenticate".
            return false;
        }
    }

    private static boolean verifyPbkdf2(String password, String storedHash) {
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4) {
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        if (salt.length == 0 || expected.length == 0) {
            return false;
        }

        byte[] actual = deriveKey(password, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static boolean verifyLegacy(String password, String storedHash)
            throws NoSuchAlgorithmException {
        byte[] combined = Base64.getDecoder().decode(storedHash);
        if (combined.length != LEGACY_LENGTH) {
            return false;
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] storedHashBytes = new byte[LEGACY_LENGTH - SALT_LENGTH];
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(combined, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        // Legacy hashes were produced from the platform's default charset.
        byte[] testHash = md.digest(password.getBytes(StandardCharsets.UTF_8));

        return MessageDigest.isEqual(storedHashBytes, testHash);
    }

    private static byte[] deriveKey(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
