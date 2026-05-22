 package tools;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Checksum {
    private static final Logger log = Log.getLogger(Checksum.class);

    public static String getFileChecksum(File file) {
        try(InputStream objInputStream = Files.newInputStream(Paths.get(file.getPath()))) {
            return DigestUtils.md5Hex(objInputStream);
        } catch(IOException e) {
            log.fail("Failed to get file checksum! - " + e.getMessage());
            return null;
        }
    }

    public static void verifyChecksum(String expected, String actual) {
        if(!expected.equals(actual))
            log.fail("Failed to verify checksum!");
        log.info("Verified checksum successfully.");
    }

    public static void verifyChecksum(String expected, File actualFile) {
        String actual = getFileChecksum(actualFile);
        if(!expected.equals(actual))
            log.fail("Failed to verify checksum!");
        log.info("Verified checksum successfully.");
    }
}
