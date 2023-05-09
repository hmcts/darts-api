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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioOperationServiceImplTest {

    @InjectMocks
    private AudioOperationServiceImpl audioOperationService;

    private static List<AudioFileInfo> audioFileInfos;

    @Mock
    private AudioTransformConfigurationProperties audioTransformConfigurationProperties;

    @Mock
    private AudioUtil audioUtil;

    @BeforeEach
    void beforeEach() {
        audioFileInfos = new ArrayList<>();
        audioFileInfos.add(new AudioFileInfo(
            Instant.parse("2023-04-28T09:00:00Z"),
            Instant.parse("2023-04-28T10:30:00Z"),
            "sample1-5secs.mp2",
            1
        ));
        audioFileInfos.add(new AudioFileInfo(
            Instant.parse("2023-04-28T10:30:00Z"),
            Instant.parse("2023-04-28T11:00:00Z"),
            "sample2-5secs.mp2",
            1
        ));
    }

    @Test
    void shouldGenerateConcatenateCommandWhenValidAudioFilesAreReceived() {
        when(audioTransformConfigurationProperties.getFfmpegExecutable()).thenReturn("/tempDir/ffmpeg");
        CommandLine expectedCommand = CommandLine.parse(
            "/tempDir/ffmpeg -i /tempDir/concat/sample1-5secs.mp2 -i /tempDir/concat/sample2-5secs.mp2"
                + " -filter_complex \"[0:a][1:a]concat=n=2:v=0:a=1\" /tempDir/concat/1-concat-out.mp2");
        CommandLine concatenateCommand = audioOperationService.generateConcatenateCommand(
            1,
            audioFileInfos,
            "/tempDir/concat"
        );
        assertNotNull(concatenateCommand);
        assertEquals(expectedCommand.getArguments().length, concatenateCommand.getArguments().length);
        assertEquals(expectedCommand.getExecutable(), concatenateCommand.getExecutable());
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioTransformConfigurationProperties.getFfmpegExecutable()).thenReturn("/tempDir/ffmpeg");
        when(audioTransformConfigurationProperties.getConcatWorkspace()).thenReturn("/tempDir/concatenate");
        when(audioUtil.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse("2023-04-28T09:00:00Z"),
            Instant.parse("2023-04-28T11:00:00Z"),
            "/tempDir/concatenate/requestId/1-concat-out.mp2",
            1
        );

        AudioFileInfo audioFileInfo = audioOperationService.concatenate("requestId", audioFileInfos);
        assertEquals(expectedAudio.getFileName(), audioFileInfo.getFileName());
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }

    @Test
    void shouldReturnMergedAudioFileInfoWhenValidInputAudioFiles() throws Exception {
        when(audioTransformConfigurationProperties.getFfmpegExecutable()).thenReturn("/tempDir/ffmpeg");
        when(audioTransformConfigurationProperties.getMergeWorkspace()).thenReturn("/tempDir");
        when(audioUtil.execute(any())).thenReturn(Boolean.TRUE);

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse("2023-04-28T09:00:00Z"),
            Instant.parse("2023-04-28T11:00:00Z"),
            "/tempDir/requestId/merge/C0-202305.mp2",
            0);

        AudioFileInfo audioFileInfo =  audioOperationService.merge(audioFileInfos, "requestId");

        String filenameExpression = "/tempDir/requestId/merge/C[0-9]-[0-9]*.mp2";
        Boolean fileNameMatch = audioFileInfo.getFileName().matches(filenameExpression);

        assertEquals(true, fileNameMatch);
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }
}
