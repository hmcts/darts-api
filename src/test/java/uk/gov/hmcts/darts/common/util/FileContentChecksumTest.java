package uk.gov.hmcts.darts.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getFile;

@Slf4j
class FileContentChecksumTest {

    private static final String MD_5 = "MD5";
    private final FileContentChecksum checksum = new FileContentChecksum();
    private static final byte[] TEST_DATA = "test".getBytes(StandardCharsets.UTF_8);
    private static final String EXPECTED_STRING_MD5_CHECKSUM = "098f6bcd4621d373cade4e832627b4f6";
    private static final String EXPECTED_AUDIO_FILE_MD5_CHECKSUM = "3fb4b2be0d3c015a6532d02d464c4262";
    private static final String EXPECTED_AZURE_AUDIO_FILE_CHECKSUM = "4d544cc2ad7a6f899c6703d29598ff89";


    @Test
    void calculateFromBytes() {
        assertEquals(EXPECTED_STRING_MD5_CHECKSUM, checksum.calculate(TEST_DATA));
    }

    @Test
    void calculateFromInputStream() throws NoSuchAlgorithmException, IOException {
        var md5Digest = MessageDigest.getInstance(MD_5);

        ByteArrayInputStream testDataInputStream = new ByteArrayInputStream(TEST_DATA);

        try (var digestInputStream = new DigestInputStream(testDataInputStream, md5Digest)) {
            // The digest is based on what has been consumed from the stream, so we must first consume the entire stream before computing the digest.
            digestInputStream.readAllBytes();
            assertEquals(EXPECTED_STRING_MD5_CHECKSUM, checksum.calculate(digestInputStream));
        }

    }

    @Test
    void calculateFromInputStreamUsingAudioFile() throws IOException, NoSuchAlgorithmException {
        var md5Digest = MessageDigest.getInstance(MD_5);
        File audioFileTest = getFile("Tests/common/util/FileContentChecksum/testAudio.mp2");
        String audioFileChecksum;
        // file hashing with DigestInputStream
        try (DigestInputStream digestInputStream = new DigestInputStream(Files.newInputStream(audioFileTest.toPath()), md5Digest)) {
            digestInputStream.readAllBytes();
            audioFileChecksum = checksum.calculate(digestInputStream);
            log.info("audioFileChecksum {}", audioFileChecksum);
        }
        assertEquals(EXPECTED_AUDIO_FILE_MD5_CHECKSUM, audioFileChecksum);
    }

    @Test
    void calculateFromInputStreamUsingAudioFileFromAzure() throws IOException, NoSuchAlgorithmException {
        var md5Digest = MessageDigest.getInstance(MD_5);
        File audioFileTest = getFile("Tests/common/util/FileContentChecksum/001b1423-1f94-4ce7-b3a8-1534eb18eb06");
        String audioFileChecksum;
        // file hashing with DigestInputStream
        try (DigestInputStream digestInputStream = new DigestInputStream(Files.newInputStream(audioFileTest.toPath()), md5Digest)) {
            digestInputStream.readAllBytes();
            audioFileChecksum = checksum.calculate(digestInputStream);
        }
        log.info("audioFileChecksum 2 {}", audioFileChecksum);
        assertEquals(EXPECTED_AZURE_AUDIO_FILE_CHECKSUM, audioFileChecksum);
    }

}
