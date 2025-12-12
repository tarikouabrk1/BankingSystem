package security.auth;

import security.hashing.SHA256Hashing;

import java.security.SecureRandom;

public final class SecurityUtils {

    private static final String SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() {
    }

    public static String generateSalt(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = RANDOM.nextInt(SALT_CHARS.length());
            sb.append(SALT_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        // Simple salted hash; in a real system you would also use many iterations
        return SHA256Hashing.hash(salt + ":" + password);
    }

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String actual = hashPassword(password, salt);
        return slowEquals(expectedHash, actual);
    }

    public static String hashPin(String pin, String salt) {
        if (pin == null || salt == null) {
            throw new IllegalArgumentException("PIN and salt must not be null");
        }
        // PIN-specific hashing; could include extra structure or iterations
        return SHA256Hashing.hash("PIN:" + salt + ":" + pin);
    }

    public static boolean verifyPin(String pin, String salt, String expectedHash) {
        String actual = hashPin(pin, salt);
        return slowEquals(expectedHash, actual);
    }

    /**
     * Constant-time string comparison to reduce timing side channels.
     */
    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}


