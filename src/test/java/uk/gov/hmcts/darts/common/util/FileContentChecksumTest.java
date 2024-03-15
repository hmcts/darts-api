package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileContentChecksumTest {

    private final FileContentChecksum checksum = new FileContentChecksum();
    private static final byte[] TEST_DATA = "test".getBytes(StandardCharsets.UTF_8);
    public static final String EXPECTED_MD5_CHECKSUM = "CY9rzUYh03PK3k6DJie09g==";

    @Test
    void calculateFromBytes() {
        assertEquals(EXPECTED_MD5_CHECKSUM, checksum.calculate(TEST_DATA));
    }

    @Test
    void calculateFromInputStream() throws NoSuchAlgorithmException, IOException {
        var md5Digest = MessageDigest.getInstance("MD5");

        ByteArrayInputStream testDataInputStream = new ByteArrayInputStream(TEST_DATA);

        try (var digestInputStream = new DigestInputStream(testDataInputStream, md5Digest)) {
            // The digest is based on what has been consumed from the stream, so we must first consume the entire stream before computing the digest.
            digestInputStream.readAllBytes();

            assertEquals(EXPECTED_MD5_CHECKSUM, checksum.calculate(digestInputStream));
        }
    }

}
