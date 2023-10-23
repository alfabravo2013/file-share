package fileshare.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5HashProvider {
    private Md5HashProvider() {}

    public static String hash(byte[] bytes) {
        try {
            var md = MessageDigest.getInstance("MD5");
            var hash = md.digest(bytes);
            var hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append("%02x".formatted(b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
