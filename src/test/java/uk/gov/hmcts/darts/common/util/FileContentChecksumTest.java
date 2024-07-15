package uk.gov.hmcts.darts.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.TestUtils.getFile;

@Slf4j
class FileContentChecksumTest {

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
    void calculateFromInputStreamUsingAudioFile() {
        File audioFileTest = getFile("Tests/common/util/FileContentChecksum/testAudio.mp2");
        String audioFileChecksum = checksum.calculate(audioFileTest.toPath());

        assertEquals(EXPECTED_AUDIO_FILE_MD5_CHECKSUM, audioFileChecksum);
    }

    @Test
    void calculateFromInputStreamUsingAudioFileFromAzure() {
        File audioFileTest = getFile("Tests/common/util/FileContentChecksum/001b1423-1f94-4ce7-b3a8-1534eb18eb06");
        String audioFileChecksum = checksum.calculate(audioFileTest.toPath());

        assertEquals(EXPECTED_AZURE_AUDIO_FILE_CHECKSUM, audioFileChecksum);
    }

    @Test
    void calculateFromInputStream() {
        ByteArrayInputStream testDataInputStream = new ByteArrayInputStream(TEST_DATA);

        var calculatedChecksum = checksum.calculate(testDataInputStream);

        assertThat(calculatedChecksum).isEqualTo(EXPECTED_STRING_MD5_CHECKSUM);
    }

}
