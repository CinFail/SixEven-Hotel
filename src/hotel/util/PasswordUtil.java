package hotel.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Utility class for SHA-256 password hashing — passwords are never stored in plain text
// Encapsulation: private constructor prevents instantiation; only static methods are exposed
public class PasswordUtil {

    private PasswordUtil() {}

    // Returns a 64-character hex digest; same input always produces same output
    public static String hash(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plainText.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String plainText, String hash) {
        return hash(plainText).equals(hash);
    }
}
