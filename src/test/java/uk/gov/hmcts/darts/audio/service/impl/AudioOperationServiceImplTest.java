package uk.gov.hmcts.darts.audio.service.impl;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.impl.SystemCommandExecutorImpl;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.test.common.FileStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioOperationServiceImplTest {

    private static final String WORKSPACE_DIR = "44887a8c-d918-4907-b9e8-38d5b1bf9c9c";
    private static final String T_09_00_00_Z = "2023-04-28T09:00:00Z";
    private static final String T_10_30_00_Z = "2023-04-28T10:30:00Z";
    private static final String T_11_00_00_Z = "2023-04-28T11:00:00Z";
    private static final String T_11_30_00_Z = "2023-04-28T11:30:00Z";
    private static final String T_10_30_12_Z = "2023-04-28T10:30:12Z";
    private static final String FFMPEG = "/usr/bin/ffmpeg";
    private static final Duration ALLOWABLE_GAP = Duration.ofSeconds(1);
    private static final Duration ALLOWABLE_GAP_MS = Duration.ofMillis(1200);

    private List<AudioFileInfo> preloadedInputAudioFileInfos;
    private Path tempDirectory;

    @InjectMocks
    private AudioOperationServiceImpl audioOperationService;

    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @Mock
    private SystemCommandExecutorImpl systemCommandExecutor;

    @BeforeEach
    void beforeEach() throws IOException {
        tempDirectory = Files.createTempDirectory("darts_api_unit_test");

        preloadedInputAudioFileInfos = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_10_30_00_Z))
                .channel(1)
                .mediaFile("original0.mp2")
                .path(createFile(tempDirectory, "original0.mp2"))
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_10_30_00_Z))
                .endTime(Instant.parse(T_11_00_00_Z))
                .channel(1)
                .mediaFile("original1.mp2")
                .path(createFile(tempDirectory, "original1.mp2"))
                .build()
        ));
    }

    @Test
    void shouldGenerateConcatenateCommandWhenValidAudioFilesAreReceived() {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);

        List<AudioFileInfo> inputAudioFileInfos = List.of(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_10_30_00_Z))
                .channel(1)
                .mediaFile("original0.mp2")
                .path(Path.of("/path/to/audio/original0.mp2"))
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_10_30_00_Z))
                .endTime(Instant.parse(T_11_00_00_Z))
                .channel(1)
                .mediaFile("original1.mp2")
                .path(Path.of("/path/to/audio/original1.mp2"))
                .build()
        );

        CommandLine actualCommand = audioOperationService.generateConcatenateCommand(
            inputAudioFileInfos,
            Path.of("/path/to/output/audio.mp2")
        );

        CommandLine expectedCommand = CommandLine.parse(
            "/usr/bin/ffmpeg -i /path/to/audio/original0.mp2 -i /path/to/audio/original1.mp2"
                + " -b:a 32k -filter_complex [0:a][1:a]concat=n=2:v=0:a=1 /path/to/output/audio.mp2");

        assertNotNull(actualCommand);
        assertEquals(expectedCommand.toString(), actualCommand.toString());
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getConcatWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo audioFileInfo = audioOperationService.concatenate(
            WORKSPACE_DIR,
            preloadedInputAudioFileInfos
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_11_00_00_Z), audioFileInfo.getEndTime());
        assertFalse(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldReturnMergedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getMergeWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo audioFileInfo = audioOperationService.merge(
            preloadedInputAudioFileInfos,
            WORKSPACE_DIR
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C0-merge-[0-9]*.mp2"));
        assertEquals(0, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_11_00_00_Z), audioFileInfo.getEndTime());
        assertFalse(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldReturnTrimmedAudioFileWhenValidInputAudioFile()
        throws ExecutionException, InterruptedException, IOException {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);

        Path tempDirectory = createTempDirectory();
        when(audioConfigurationProperties.getTrimWorkspace()).thenReturn(tempDirectory.toString());

        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);
        Path file = FileStore.getFileStore().create(tempDirectory.resolve("original.mp2")).toPath();

        AudioFileInfo audioFileInfo = audioOperationService.trim(
            WORKSPACE_DIR,
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_10_30_00_Z))
                .channel(1)
                .mediaFile("original.mp2")
                .path(file)
                .build(),
            Duration.of(45, MINUTES),
            Duration.of(75, MINUTES)
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-trim-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse("2023-04-28T09:45:00Z"), audioFileInfo.getStartTime());
        assertEquals(Instant.parse("2023-04-28T10:15:00Z"), audioFileInfo.getEndTime());
        assertTrue(audioFileInfo.isTrimmed());
    }

    @Test
    void shouldAdjustTimeDurationWhenValid() {
        AudioFileInfo audioFileInfo = preloadedInputAudioFileInfos.getFirst();
        assertEquals(
            Instant.parse(T_09_00_00_Z),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(0, SECONDS))
        );
        assertEquals(
            Instant.parse("2023-04-28T09:00:05Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(5, SECONDS))
        );
        assertEquals(
            Instant.parse("2023-04-28T09:01:30Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), Duration.of(90, SECONDS))
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
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getReEncodeWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo audioFileInfo = audioOperationService.reEncode(
            WORKSPACE_DIR,
            preloadedInputAudioFileInfos.getFirst()
        );

        assertTrue(audioFileInfo.getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[0-4]-encode-[0-9]*.mp3"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_10_30_00_Z), audioFileInfo.getEndTime());
        assertFalse(audioFileInfo.isTrimmed());
    }

    private Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("darts_api_unit_test");
    }

    private Path createFile(Path path, String name) throws IOException {
        return FileStore.getFileStore().create(path, Path.of(name)).toPath();
    }

    @Test
    void shouldReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveGap() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getConcatWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        List<AudioFileInfo> inputAudioFileInfosWithGaps = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_10_30_00_Z))
                .channel(1)
                .mediaFile("original2.mp2")
                .path(createFile(tempDirectory, "original2.mp2"))
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_11_00_00_Z))
                .endTime(Instant.parse(T_11_30_00_Z))
                .channel(1)
                .mediaFile("original3.mp2")
                .path(createFile(tempDirectory, "original3.mp2"))
                .build()
        ));

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            inputAudioFileInfosWithGaps,
            ALLOWABLE_GAP
        );

        assertTrue(audioFileInfo.getFirst().getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getFirst().getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getFirst().getStartTime());
        assertEquals(Instant.parse(T_10_30_00_Z), audioFileInfo.getFirst().getEndTime());
        assertEquals(Instant.parse(T_11_00_00_Z), audioFileInfo.get(1).getStartTime());
        assertEquals(Instant.parse(T_11_30_00_Z), audioFileInfo.get(1).getEndTime());
        assertFalse(audioFileInfo.getFirst().isTrimmed());
    }

    @Test
    void shouldReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveGapWithSeconds() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getConcatWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        List<AudioFileInfo> inputAudioFileInfosWithSecondGaps = new ArrayList<>(Arrays.asList(
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_09_00_00_Z))
                .endTime(Instant.parse(T_10_30_00_Z))
                .channel(1)
                .mediaFile("original4.mp2")
                .path(createFile(tempDirectory, "original4.mp2"))
                .build(),
            AudioFileInfo.builder()
                .startTime(Instant.parse(T_10_30_12_Z))
                .endTime(Instant.parse(T_11_30_00_Z))
                .channel(1)
                .mediaFile("original5.mp2")
                .path(createFile(tempDirectory, "original5.mp2"))
                .build()
        ));

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            inputAudioFileInfosWithSecondGaps,
            ALLOWABLE_GAP_MS
        );

        assertTrue(audioFileInfo.getFirst().getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getFirst().getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getFirst().getStartTime());
        assertEquals(Instant.parse(T_10_30_00_Z), audioFileInfo.getFirst().getEndTime());
        assertEquals(Instant.parse(T_10_30_12_Z), audioFileInfo.get(1).getStartTime());
        assertEquals(Instant.parse(T_11_30_00_Z), audioFileInfo.get(1).getEndTime());
        assertFalse(audioFileInfo.getFirst().isTrimmed());
    }

    @Test
    void shouldReturnConcatenatedAudioFileListInfoWhenValidInputAudioFilesHaveNoGap() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getConcatWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenateWithGaps(
            WORKSPACE_DIR,
            preloadedInputAudioFileInfos,
            ALLOWABLE_GAP
        );

        assertTrue(audioFileInfo.getFirst().getPath().toString().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getFirst().getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getFirst().getStartTime());
        assertEquals(Instant.parse(T_11_00_00_Z), audioFileInfo.getFirst().getEndTime());
        assertEquals(1, audioFileInfo.size());
        assertFalse(audioFileInfo.getFirst().isTrimmed());
    }
}