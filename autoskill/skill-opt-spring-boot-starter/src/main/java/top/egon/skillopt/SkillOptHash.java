package top.egon.skillopt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class SkillOptHash {

    private SkillOptHash() {
    }

    static String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(path));
            StringBuilder builder = new StringBuilder("sha256:");
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
        catch (IOException | NoSuchAlgorithmException ex) {
            return "";
        }
    }
}
