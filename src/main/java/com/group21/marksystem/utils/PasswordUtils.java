package com.group21.marksystem.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256 with salt.
 *
 * @author Group 21
 */
public class PasswordUtils {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Hashes a password using SHA-256 with a random salt.
     *
     * @param password The plain text password to hash.
     * @return A Base64-encoded string containing salt and hashed password.
     */
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Combine salt + hash
            byte[] combined = new byte[48];
            System.arraycopy(salt, 0, combined, 0, 16);
            System.arraycopy(hashedPassword, 0, combined, 16, 32);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a password against a stored hash.
     *
     * @param password The plain text password to verify.
     * @param storedHash The stored hash (Base64-encoded salt + hash).
     * @return true if the password matches the stored hash.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            byte[] combined = Base64.getDecoder().decode(storedHash);

            byte[] salt = new byte[16];
            byte[] storedHashBytes = new byte[32];
            System.arraycopy(combined, 0, salt, 0, 16);
            System.arraycopy(combined, 16, storedHashBytes, 0, 32);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] testHash = md.digest(password.getBytes());

            return MessageDigest.isEqual(storedHashBytes, testHash);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }
}
