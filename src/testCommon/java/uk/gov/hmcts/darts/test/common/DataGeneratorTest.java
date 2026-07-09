package uk.gov.hmcts.darts.test.common;


import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataGeneratorTest {

    @Test
    void shouldProduceAValidMP2() throws IOException {
        Path audioFile = DataGenerator.createUniqueFile(DataSize.ofBytes(100), DataGenerator.FileType.MP2);

        assertEquals(100, audioFile.toFile().length());
        assertEquals("audio/mpeg", new Tika().detect(audioFile));
    }

    @Test
    void shouldProduceAValidMP3() throws IOException {
        Path uniqueAudioFile = DataGenerator.createUniqueFile(DataSize.ofBytes(100), DataGenerator.FileType.MP3);

        assertEquals(100, uniqueAudioFile.toFile().length());
        assertEquals("audio/mpeg", new Tika().detect(uniqueAudioFile));
    }

    @Test
    void showThrowExceptionIfDesiredLengthIsTooSmall() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
            DataGenerator.createUniqueFile(DataSize.ofBytes(1), DataGenerator.FileType.MP2)
        );

        assertEquals("The provided dataSize must be at least equal to the length of the file signature (2 bytes)",
                     illegalArgumentException.getMessage());
    }

}
