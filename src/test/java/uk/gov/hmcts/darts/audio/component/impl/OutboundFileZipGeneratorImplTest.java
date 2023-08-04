package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
@ExtendWith(MockitoExtension.class)
class OutboundFileZipGeneratorImplTest {

    // Any value that exceeds the size of the read buffer of the tested impl
    private static final int DUMMY_FILE_SIZE = 2048;
    private static final Instant SOME_INSTANT = Instant.now();

    private OutboundFileZipGeneratorImpl outboundFileZipGenerator;
    private Path tempDirectory;

    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @BeforeEach
    void setUp() throws IOException {
        outboundFileZipGenerator = new OutboundFileZipGeneratorImpl(audioConfigurationProperties);

        var tempDirectoryName = UUID.randomUUID().toString();
        tempDirectory = Files.createTempDirectory(tempDirectoryName);

        when(audioConfigurationProperties.getTempBlobWorkspace())
            .thenReturn(tempDirectory.toString());
    }

    @Test
    void generateAndWriteZipShouldProduceZipWithTheExpectedFileStructure() {
        var audioWithSession1AndChannel1 = createDummyFileAndAudioFileInfo(1);
        var audioWithSession1AndChannel2 = createDummyFileAndAudioFileInfo(2);
        List<AudioFileInfo> session1 = List.of(
            audioWithSession1AndChannel1,
            audioWithSession1AndChannel2
        );

        var audioWithSession2AndChannel1 = createDummyFileAndAudioFileInfo(1);
        List<AudioFileInfo> session2 = List.of(
            audioWithSession2AndChannel1
        );

        Path path = outboundFileZipGenerator.generateAndWriteZip(List.of(session1, session2));

        assertTrue(Files.exists(path));

        List<String> paths = readZipStructure(path);

        assertEquals(3, paths.size());
        assertThat(paths, hasItem("0001/0001.a00"));
        assertThat(paths, hasItem("0001/0001.a01"));
        assertThat(paths, hasItem("0002/0002.a00"));
    }

    private AudioFileInfo createDummyFileAndAudioFileInfo(int channel) {
        Path path = createDummyFile();
        return new AudioFileInfo(SOME_INSTANT, SOME_INSTANT, path.toString(), channel);
    }

    private Path createDummyFile() {
        var tempFilename = UUID.randomUUID()
            .toString();
        try {
            return Files.write(tempDirectory.resolve(tempFilename), new byte[DUMMY_FILE_SIZE]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private List<String> readZipStructure(Path path) {
        List<String> paths = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                paths.add(entry.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return paths;
    }

}
