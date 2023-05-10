package uk.gov.hmcts.darts.audio.service.impl;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.util.AudioUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioOperationServiceImplTest {

    private static final String WORKSPACE_DIR = "requestId";
    private static final String T_09_00_00_Z = "2023-04-28T09:00:00Z";
    private static final String T_10_30_00_Z = "2023-04-28T10:30:00Z";
    private static final String T_11_00_00_Z = "2023-04-28T11:00:00Z";

    private List<AudioFileInfo> audioFileInfos;

    @InjectMocks
    private AudioOperationServiceImpl audioOperationService;

    @Mock
    private AudioTransformConfigurationProperties audioTransformConfigurationProperties;

    @Mock
    private AudioUtil audioUtil;

    @BeforeEach
    void beforeEach() {
        when(audioTransformConfigurationProperties.getFfmpegExecutable()).thenReturn("/usr/bin/ffmpeg");

        audioFileInfos = new ArrayList<>();
        audioFileInfos.add(new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_10_30_00_Z),
            "sample1-5secs.mp2",
            1
        ));
        audioFileInfos.add(new AudioFileInfo(
            Instant.parse(T_10_30_00_Z),
            Instant.parse(T_11_00_00_Z),
            "sample2-5secs.mp2",
            1
        ));
    }

    @Test
    void shouldGenerateConcatenateCommandWhenValidAudioFilesAreReceived() {
        CommandLine expectedCommand = CommandLine.parse(
            "/usr/bin/ffmpeg -i /tempDir/concatenate/requestId/sample1-5secs.mp2 -i /tempDir/concatenate/requestId/sample2-5secs.mp2"
                + " -filter_complex \"[0:a][1:a]concat=n=2:v=0:a=1\" /tempDir/concatenate/requestId/C1-concatenate-20230510145233697.mp2");

        CommandLine concatenateCommand = audioOperationService.generateConcatenateCommand(
            1,
            audioFileInfos,
            "/tempDir/concatenate/requestId",
            "C1-concatenate-20230510145233697.mp2"
        );

        assertNotNull(concatenateCommand);
        assertEquals(expectedCommand.getArguments().length, concatenateCommand.getArguments().length);
        assertEquals(expectedCommand.getExecutable(), concatenateCommand.getExecutable());
        assertEquals(expectedCommand.toString(), concatenateCommand.toString());
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioTransformConfigurationProperties.getConcatWorkspace()).thenReturn("/tempDir/concatenate");
        when(audioUtil.execute(any())).thenReturn(Boolean.TRUE);

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
        when(audioTransformConfigurationProperties.getMergeWorkspace()).thenReturn("/tempDir");
        when(audioUtil.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_11_00_00_Z),
            "/tempDir/requestId/merge/C0-202305.mp2",
            0
        );

        AudioFileInfo audioFileInfo = audioOperationService.merge(audioFileInfos, WORKSPACE_DIR);

        assertTrue(audioFileInfo.getFileName().matches("/tempDir/requestId/C0-merge-[0-9]*.mp2"));
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

    @Test
    void shouldReturnTrimmedAudioFileWhenValidInputAudioFile() throws ExecutionException, InterruptedException {
        when(audioTransformConfigurationProperties.getTrimWorkspace()).thenReturn("/tempDir/trim");
        when(audioUtil.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse(T_09_00_00_Z),
            Instant.parse(T_10_30_00_Z),
            "/tempDir/trim/requestId/trim/C1-20230510123741468.mp2",
            1
        );

        AudioFileInfo audioFileInfo = audioOperationService.trim(WORKSPACE_DIR, audioFileInfos.get(0), "0", "5");

        assertTrue(audioFileInfo.getFileName().matches("/tempDir/trim/requestId/C[1-4]-trim-[0-9]*.mp2"));
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

}
