package uk.gov.hmcts.darts.audio.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class AudioOperationServiceIntTest extends IntegrationBase {

    private static final String WORKSPACE_DIR = "44887a8c-d918-4907-b9e8-38d5b1bf9c9c";
    private static final String T_09_00_00_Z = "2023-04-28T09:00:00Z";
    private static final String T_09_01_00_Z = "2023-04-28T09:01:00Z";
    private static final String T_09_02_00_Z = "2023-04-28T09:02:00Z";
    private static final String T_09_02_01_Z = "2023-04-28T09:02:01Z";
    private static final String T_09_03_00_Z = "2023-04-28T09:03:00Z";
    private static final String T_09_03_01_Z = "2023-04-28T09:03:01Z";
    private static final String T_12_00_00_Z = "2023-04-28T12:00:00Z";
    private static final String T_12_01_00_Z = "2023-04-28T12:01:00Z";

    private static final Duration ALLOWABLE_GAP = Duration.ofSeconds(1);
    private static final Duration ALLOWABLE_GAP_MS = Duration.ofMillis(1200);
    private static final String AUDIO_FILENAME1 = "tests/audio/WithViqHeader/viq0001min.mp2";
    private static final String AUDIO_FILENAME2 = "tests/audio/WithViqHeader/1_to_2m.mp2";
    public static final String COMMAND_INPUT = " -i ";
    public static final String FFMPEG = "ffmpeg";

    private List<AudioFileInfo> preloadedInputAudioFileInfos;
    private Path tempDirectory;

    @Autowired
    private AudioOperationServiceImpl audioOperationService;


    @BeforeEach
    void beforeEach() throws IOException {
        UUID externalLocation = UUID.randomUUID();
        tempDirectory = Files.createTempDirectory(externalLocation + "darts_api_unit_test");

        File audioFileTest1 = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest1.toPath(), createFile(tempDirectory, "original0.mp2"), REPLACE_EXISTING);
        File audioFileTest2 = TestUtils.getFile(AUDIO_FILENAME2);
        Path path2 = Files.copy(audioFileTest2.toPath(), createFile(tempDirectory, "original1.mp2"), REPLACE_EXISTING);

        preloadedInputAudioFileInfos = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(1)
                .mediaFile("original0.mp2")
                .path(path1)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_01_00_Z))
                .endTime(Instant.parse(T_09_02_00_Z))
                .channel(1)
                .mediaFile("original1.mp2")
                .path(path2)
                .build()
        ));
    }

    @Test
    void shouldGenerateConcatenateCommandWhenValidAudioFilesAreReceived() throws IOException {

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test1.mp2"), REPLACE_EXISTING);
        Path path2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test2.mp2"), REPLACE_EXISTING);

        List<AudioFileInfo> inputAudioFileInfos = List.of(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(1)
                .mediaFile("test1.mp2")
                .path(path1)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_01_00_Z))
                .endTime(Instant.parse(T_09_02_00_Z))
                .channel(1)
                .mediaFile("test2.mp2")
                .path(path2)
                .build()
        );

        Path outputPath = tempDirectory.resolve("/audio.mp2");
        CommandLine actualCommand = audioOperationService.generateConcatenateCommand(
            inputAudioFileInfos,
            outputPath
        );

        StringBuilder command = new StringBuilder(52);
        command.append(FFMPEG)
            .append(COMMAND_INPUT).append(path1)
            .append(COMMAND_INPUT).append(path2)
            .append(" -b:a 32k -filter_complex [0:a][1:a]concat=n=2:v=0:a=1 ")
            .append(outputPath);
        CommandLine expectedCommand = CommandLine.parse(command.toString());

        assertNotNull(actualCommand);
        assertEquals(expectedCommand.toString(), actualCommand.toString());
    }

    @Test
    void shouldGenerateConcatenateCommandWhenMultipleChannelsAreReceived() throws IOException {

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test1.mp2"), REPLACE_EXISTING);
        Path path2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test2.mp2"), REPLACE_EXISTING);
        Path path3 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test3.mp2"), REPLACE_EXISTING);
        Path path4 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test4.mp2"), REPLACE_EXISTING);

        List<AudioFileInfo> inputAudioFileInfos = List.of(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(1)
                .mediaFile("test1.mp2")
                .path(path1)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(2)
                .mediaFile("test2.mp2")
                .path(path2)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(3)
                .mediaFile("test3.mp2")
                .path(path3)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(4)
                .mediaFile("test4.mp2")
                .path(path4)
                .build()
        );

        Path outputPath = tempDirectory.resolve("/audio.mp2");
        CommandLine actualCommand = audioOperationService.generateConcatenateCommand(
            inputAudioFileInfos,
            outputPath
        );

        StringBuilder command = new StringBuilder(62);
        command.append(FFMPEG)
            .append(COMMAND_INPUT).append(path1)
            .append(COMMAND_INPUT).append(path2)
            .append(COMMAND_INPUT).append(path3)
            .append(COMMAND_INPUT).append(path4)
            .append(" -b:a 32k -filter_complex [0:a][1:a][2:a][3:a]concat=n=4:v=0:a=1 ")
            .append(outputPath);
        CommandLine expectedCommand = CommandLine.parse(command.toString());

        assertNotNull(actualCommand);
        assertEquals(expectedCommand.toString(), actualCommand.toString());
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        AudioFileInfo audioFileInfo = audioOperationService.concatenate(
            WORKSPACE_DIR,
            preloadedInputAudioFileInfos
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_09_02_00_Z), audioFileInfo.getEndTime());
        assertFalse(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldReturnMergedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        AudioFileInfo audioFileInfo = audioOperationService.merge(
            preloadedInputAudioFileInfos,
            WORKSPACE_DIR
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C0-merge-[0-9]*.mp2"));
        assertEquals(0, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_09_02_00_Z), audioFileInfo.getEndTime());
        assertFalse(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldReturnTrimmedAudioFileWhenValidInputAudioFile()
        throws ExecutionException, InterruptedException, IOException {

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test1.mp2"), REPLACE_EXISTING);

        AudioFileInfo audioFileInfo = audioOperationService.trim(
            WORKSPACE_DIR,
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(1)
                .mediaFile("test1.mp2")
                .path(path1)
                .build(),
            Duration.of(15, SECONDS),
            Duration.of(45, SECONDS)
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-trim-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse("2023-04-28T09:00:15Z"), audioFileInfo.getStartTime());
        assertEquals(Instant.parse("2023-04-28T09:00:45Z"), audioFileInfo.getEndTime());
        assertTrue(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldAdjustTimeDurationWhenValid() {
        AudioFileInfo audioFileInfo = preloadedInputAudioFileInfos.get(0);
        assertEquals(
            Instant.parse(T_09_00_00_Z),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(0, SECONDS))
        );
        assertEquals(
            Instant.parse("2023-04-28T09:00:05Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(5, SECONDS))
        );
        assertEquals(
            Instant.parse("2023-04-28T09:00:30Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(30, SECONDS))
        );
        assertEquals(
            Instant.parse("2023-04-28T10:15:00Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(75, MINUTES))
        );

        assertEquals(
            Instant.parse("2023-05-12T15:05:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-05-12T14:20:00Z"), Duration.of(45, MINUTES))
        );
        assertEquals(
            Instant.parse("2023-05-13T01:00:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-05-12T23:20:00Z"), Duration.of(100, MINUTES))
        );
        assertEquals(
            Instant.parse("2023-03-26T01:30:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-03-25T23:30:00Z"), Duration.of(2, HOURS))
        );
        assertEquals(
            Instant.parse("2023-10-29T02:15:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-10-29T00:30:00Z"), Duration.of(105, MINUTES))
        );
        assertEquals(
            Instant.parse("2023-04-28T08:59:00Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(-1, MINUTES))
        );
    }

    @Test
    void shouldReturnReEncodedAudioFileInfoWhenValidInputAudioFile()
        throws ExecutionException, InterruptedException, IOException {

        AudioFileInfo audioFileInfo = audioOperationService.reEncode(
            WORKSPACE_DIR,
            preloadedInputAudioFileInfos.get(0)
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[0-4]-encode-[0-9]*.mp3"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_09_01_00_Z), audioFileInfo.getEndTime());
        assertFalse(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveGap() throws Exception {
        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test2.mp2"), REPLACE_EXISTING);
        Path path2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test3.mp2"), REPLACE_EXISTING);

        List<AudioFileInfo> inputAudioFileInfosWithGaps = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_01_00_Z))
                .endTime(Instant.parse(T_09_02_00_Z))
                .channel(1)
                .mediaFile("test2.mp2")
                .path(path1)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_02_01_Z))
                .endTime(Instant.parse(T_09_03_01_Z))
                .channel(1)
                .mediaFile("test3.mp2")
                .path(path2)
                .build()
        ));

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            inputAudioFileInfosWithGaps,
            ALLOWABLE_GAP
        );

        assertTrue(audioFileInfo.get(0).getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.get(0).getChannel());
        assertEquals(Instant.parse(T_09_01_00_Z), audioFileInfo.get(0).getStartTime());
        assertEquals(Instant.parse(T_09_03_01_Z), audioFileInfo.get(0).getEndTime());
        assertFalse(audioFileInfo.get(0).isTrimmed());
    }

    @Test
    void shouldReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveGapWithSeconds() throws Exception {

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "original4.mp2"), REPLACE_EXISTING);
        Path path2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "original5.mp2"), REPLACE_EXISTING);

        List<AudioFileInfo> inputAudioFileInfosWithSecondGaps = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(1)
                .mediaFile("original4.mp2")
                .path(path1)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_02_00_Z))
                .endTime(Instant.parse(T_09_03_00_Z))
                .channel(1)
                .mediaFile("original5.mp2")
                .path(path2)
                .build()
        ));

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            inputAudioFileInfosWithSecondGaps,
            ALLOWABLE_GAP_MS
        );

        assertTrue(audioFileInfo.get(0).getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.get(0).getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.get(0).getStartTime());
        assertEquals(Instant.parse(T_09_01_00_Z), audioFileInfo.get(0).getEndTime());
        assertEquals(Instant.parse(T_09_02_00_Z), audioFileInfo.get(1).getStartTime());
        assertEquals(Instant.parse(T_09_03_00_Z), audioFileInfo.get(1).getEndTime());
        assertFalse(audioFileInfo.get(0).isTrimmed());
    }

    @Test
    void shouldNotReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveLargeGap() throws Exception {
        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME1);
        Path path1 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test6.mp2"), REPLACE_EXISTING);
        Path path2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "test7.mp2"), REPLACE_EXISTING);

        List<AudioFileInfo> inputAudioFileInfosWithGaps = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_09_01_00_Z))
                .channel(1)
                .mediaFile("test6.mp2")
                .path(path1)
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_12_00_00_Z))
                .endTime(Instant.parse(T_12_01_00_Z))
                .channel(1)
                .mediaFile("test7.mp2")
                .path(path2)
                .build()
        ));

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            inputAudioFileInfosWithGaps,
            ALLOWABLE_GAP
        );

        assertTrue(audioFileInfo.get(0).getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.get(0).getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.get(0).getStartTime());
        assertEquals(Instant.parse(T_09_01_00_Z), audioFileInfo.get(0).getEndTime());
        assertFalse(audioFileInfo.get(0).isTrimmed());
    }

    @Test
    void shouldReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveNoGap() throws Exception {

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            preloadedInputAudioFileInfos,
            ALLOWABLE_GAP
        );

        assertTrue(audioFileInfo.get(0).getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.get(0).getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.get(0).getStartTime());
        assertEquals(Instant.parse(T_09_02_00_Z), audioFileInfo.get(0).getEndTime());
        assertEquals(1, audioFileInfo.size());
        assertFalse(audioFileInfo.get(0).isTrimmed());
    }

    private Path createFile(Path path, String name) throws IOException {
        return Files.createFile(path.resolve(name));
    }

    @AfterEach
    void deleteFile() {
        if (tempDirectory != null) {
            try {
                FileUtils.forceDelete(tempDirectory.toFile());
            } catch (IOException e) {
                log.error("Unable to delete directory {}", tempDirectory);
            }
        }
    }
}
