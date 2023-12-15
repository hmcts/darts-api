package uk.gov.hmcts.darts.audio.service.impl;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.component.impl.SystemCommandExecutorImpl;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.helper.AudioSessionHelper;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

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
    private static final String FFMPEG = "/usr/bin/ffmpeg";

    private List<AudioFileInfo> inputAudioFileInfos;
    private Path tempDirectory;

    @InjectMocks
    private AudioOperationServiceImpl audioOperationService;

    @Mock
    private AudioSessionHelper audioSessionHelper;

    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @Mock
    private SystemCommandExecutorImpl systemCommandExecutor;

    @BeforeEach
    void beforeEach() throws IOException {
        tempDirectory = Files.createTempDirectory("darts_api_unit_test");

        inputAudioFileInfos = List.of(
            new AudioFileInfo(
                Instant.parse(T_09_00_00_Z),
                Instant.parse(T_10_30_00_Z),
                createFile(tempDirectory, "original0.mp3").toString(),
                1,null
            ),
            new AudioFileInfo(
                Instant.parse(T_10_30_00_Z),
                Instant.parse(T_11_00_00_Z),
                createFile(tempDirectory, "original1.mp3").toString(),
                1,null
            )
        );

        audioOperationService = new AudioOperationServiceImpl(audioConfigurationProperties, systemCommandExecutor, audioSessionHelper);
    }

    @Test
    void shouldGenerateConcatenateCommandWhenValidAudioFilesAreReceived() {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);

        List<AudioFileInfo> inputAudioFileInfos = List.of(
            new AudioFileInfo(
                Instant.parse(T_09_00_00_Z),
                Instant.parse(T_10_30_00_Z),
                "/path/to/audio/original0.mp3",
                1,null
            ),
            new AudioFileInfo(
                Instant.parse(T_10_30_00_Z),
                Instant.parse(T_11_00_00_Z),
                "/path/to/audio/original1.mp3",
                1,null
            )
        );

        CommandLine actualCommand = audioOperationService.generateConcatenateCommand(
            inputAudioFileInfos,
            Path.of("/path/to/output/audio.mp2")
        );

        CommandLine expectedCommand = CommandLine.parse(
            "/usr/bin/ffmpeg -i /path/to/audio/original0.mp3 -i /path/to/audio/original1.mp3"
                + " -filter_complex [0:a][1:a]concat=n=2:v=0:a=1 /path/to/output/audio.mp2");

        assertNotNull(actualCommand);
        assertEquals(expectedCommand.toString(), actualCommand.toString());
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getConcatWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        List<List<AudioFileInfo>> audioFileInfos = new ArrayList<>(Arrays.asList(inputAudioFileInfos));
        when(audioSessionHelper.getSeparatedAudioFileInfo(inputAudioFileInfos,1)).thenReturn(audioFileInfos);

        List<AudioFileInfo> audioFileInfo = audioOperationService.concatenate(
            WORKSPACE_DIR,
            inputAudioFileInfos,
            1
        );

        assertTrue(audioFileInfo.get(0).getFileName().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.get(0).getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.get(0).getStartTime());
        assertEquals(Instant.parse(T_11_00_00_Z), audioFileInfo.get(0).getEndTime());
    }

    @Test
    void shouldReturnMergedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getMergeWorkspace()).thenReturn(tempDirectory.toString());
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo audioFileInfo = audioOperationService.merge(
            inputAudioFileInfos,
            WORKSPACE_DIR
        );

        assertTrue(audioFileInfo.getFileName().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C0-merge-[0-9]*.mp2"));
        assertEquals(0, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_11_00_00_Z), audioFileInfo.getEndTime());
    }

    @Test
    void shouldReturnTrimmedAudioFileWhenValidInputAudioFile()
        throws ExecutionException, InterruptedException, IOException {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);

        Path tempDirectory = createTempDirectory();
        when(audioConfigurationProperties.getTrimWorkspace()).thenReturn(tempDirectory.toString());

        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);
        Path file = Files.createFile(tempDirectory.resolve("original.mp3"));

        AudioFileInfo audioFileInfo = audioOperationService.trim(
            WORKSPACE_DIR,
            new AudioFileInfo(
                Instant.parse(T_09_00_00_Z),
                Instant.parse(T_10_30_00_Z),
                file.toString(),
                1,null
            ),
            Duration.of(45, MINUTES),
            Duration.of(75, MINUTES)
        );

        assertTrue(audioFileInfo.getFileName().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[1-4]-trim-[0-9]*.mp2"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse("2023-04-28T09:45:00Z"), audioFileInfo.getStartTime());
        assertEquals(Instant.parse("2023-04-28T10:15:00Z"), audioFileInfo.getEndTime());
    }

    @Test
    void shouldAdjustTimeDurationWhenValid() {
        AudioFileInfo audioFileInfo = inputAudioFileInfos.get(0);
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
            inputAudioFileInfos.get(0)
        );

        assertTrue(audioFileInfo.getFileName().matches(".*/44887a8c-d918-4907-b9e8-38d5b1bf9c9c/C[0-4]-encode-[0-9]*.mp3"));
        assertEquals(1, audioFileInfo.getChannel());
        assertEquals(Instant.parse(T_09_00_00_Z), audioFileInfo.getStartTime());
        assertEquals(Instant.parse(T_10_30_00_Z), audioFileInfo.getEndTime());
    }

    private Path createTempDirectory() throws IOException {
        return Files.createTempDirectory("darts_api_unit_test");
    }

    private Path createFile(Path path, String name) throws IOException {
        return Files.createFile(path.resolve(name));
    }

}
