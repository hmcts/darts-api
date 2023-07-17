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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioOperationServiceImplTest {

    private static final String WORKSPACE_DIR = "requestId";
    private static final String T_09_00_00_Z = "2023-04-28T09:00:00Z";
    private static final String T_10_30_00_Z = "2023-04-28T10:30:00Z";
    private static final String T_11_00_00_Z = "2023-04-28T11:00:00Z";
    private static final String FFMPEG = "/usr/bin/ffmpeg";

    private List<AudioFileInfo> audioFileInfos;

    @InjectMocks
    private AudioOperationServiceImpl audioOperationService;

    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @Mock
    private SystemCommandExecutorImpl systemCommandExecutor;

    @BeforeEach
    void beforeEach() {
        audioFileInfos = new ArrayList<>();
        audioFileInfos.add(new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_10_30_00_Z),
            "/path/to/audio/requestId/sample1-5secs.mp2",
            1
        ));
        audioFileInfos.add(new AudioFileInfo(
            Instant.parse(T_10_30_00_Z),
            Instant.parse(T_11_00_00_Z),
            "/path/to/audio/requestId/sample2-5secs.mp2",
            1
        ));
    }

    @Test
    void shouldGenerateConcatenateCommandWhenValidAudioFilesAreReceived() {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);

        CommandLine expectedCommand = CommandLine.parse(
            "/usr/bin/ffmpeg -i /path/to/audio/requestId/sample1-5secs.mp2 -i /path/to/audio/requestId/sample2-5secs.mp2"
                + " -filter_complex \"[0:a][1:a]concat=n=2:v=0:a=1\" /tempDir/concatenate/requestId/C1-concatenate-20230510145233697.mp2");

        CommandLine concatenateCommand = audioOperationService.generateConcatenateCommand(
            1,
            audioFileInfos,
            "/tempDir/concatenate/requestId/C1-concatenate-20230510145233697.mp2"
        );

        assertNotNull(concatenateCommand);
        assertEquals(expectedCommand.getArguments().length, concatenateCommand.getArguments().length);
        assertEquals(expectedCommand.getExecutable(), concatenateCommand.getExecutable());
        assertEquals(expectedCommand.toString(), concatenateCommand.toString());
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getConcatWorkspace()).thenReturn("/tempDir/concatenate");
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_11_00_00_Z),
            "/tempDir/concatenate/requestId/C1-concatenate-20230510145233697.mp2",
            1
        );

        AudioFileInfo audioFileInfo = audioOperationService.concatenate(WORKSPACE_DIR, audioFileInfos);

        assertTrue(audioFileInfo.getFileName().matches("/tempDir/concatenate/requestId/C[1-4]-concatenate-[0-9]*.mp2"));
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

    @Test
    void shouldReturnMergedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getMergeWorkspace()).thenReturn("/tempDir/merge");
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_11_00_00_Z),
            "/tempDir/merge/requestId/C0-merge-20230510145233697.mp2",
            0
        );

        AudioFileInfo audioFileInfo = audioOperationService.merge(audioFileInfos, WORKSPACE_DIR);

        assertTrue(audioFileInfo.getFileName().matches("/tempDir/merge/requestId/C0-merge-[0-9]*.mp2"));
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

    @Test
    void shouldReturnTrimmedAudioFileWhenValidInputAudioFile() throws ExecutionException, InterruptedException {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getTrimWorkspace()).thenReturn("/tempDir/trim");
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse("2023-04-28T09:45:00Z"),
            Instant.parse("2023-04-28T10:15:00Z"),
            "/tempDir/trim/requestId/C1-trim-20230510123741468.mp2",
            1
        );

        AudioFileInfo audioFileInfo = audioOperationService.trim(
            WORKSPACE_DIR,
            audioFileInfos.get(0),
            "00:45:00",
            "01:15:00"
        );

        assertTrue(audioFileInfo.getFileName().matches("/tempDir/trim/requestId/C[1-4]-trim-[0-9]*.mp2"));
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

    @Test
    void shouldAdjustTimeDurationWhenValid() {
        AudioFileInfo audioFileInfo = audioFileInfos.get(0);
        assertEquals(
            Instant.parse(T_09_00_00_Z),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), "00:00:00")
        );
        assertEquals(
            Instant.parse("2023-04-28T09:00:05Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), "00:00:05")
        );
        assertEquals(
            Instant.parse("2023-04-28T09:01:30Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), "00:01:30")
        );
        assertEquals(
            Instant.parse("2023-04-28T10:15:00Z"),
            audioOperationService.adjustTimeDuration(audioFileInfo.getStartTime(), "01:15:00")
        );

        assertEquals(
            Instant.parse("2023-05-12T15:05:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-05-12T14:20:00Z"), "00:45:00")
        );
        assertEquals(
            Instant.parse("2023-05-13T01:00:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-05-12T23:20:00Z"), "01:40:00")
        );
        assertEquals(
            Instant.parse("2023-03-26T01:30:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-03-25T23:30:00Z"), "02:00:00")
        );
        assertEquals(
            Instant.parse("2023-10-29T02:15:00Z"),
            audioOperationService.adjustTimeDuration(Instant.parse("2023-10-29T00:30:00Z"), "01:45:00")
        );
    }

    @Test
    void shouldThrowExceptionWhenAdjustTimeDurationNotValid() {
        assertThrows(
            DateTimeParseException.class,
            () -> audioOperationService.adjustTimeDuration(Instant.parse("2023-05-12T10:00:00Z"), "60")
        );
    }

    @Test
    void shouldReturnReEncodedAudioFileInfoWhenValidInputAudioFile() throws ExecutionException, InterruptedException {
        when(audioConfigurationProperties.getFfmpegExecutable()).thenReturn(FFMPEG);
        when(audioConfigurationProperties.getReEncodeWorkspace()).thenReturn("/tempDir/encode");
        when(systemCommandExecutor.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_10_30_00_Z),
            "/tempDir/encode/requestId/C0-encode-20230512163422198.mp3",
            0
        );

        AudioFileInfo audioFileInfo = audioOperationService.reEncode(
            WORKSPACE_DIR,
            new AudioFileInfo(
                Instant.parse(T_09_00_00_Z),
                Instant.parse(T_10_30_00_Z),
                "/tempDir/merge/requestId/C0-merge-20230510145233697.mp2",
                0
            )
        );

        assertTrue(audioFileInfo.getFileName().matches("/tempDir/encode/requestId/C[0-4]-encode-[0-9]*.mp3"));
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

}
