package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioOperationServiceImplTest {

    @InjectMocks
    private AudioOperationServiceImpl audioOperationService;

    private static List<AudioFileInfo> audioFileInfos;

    @Mock
    private AudioTransformConfigurationProperties audioTransformConfigurationProperties;

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
        String expectedCommand = "/tempDir/ffmpeg -i sample1-5secs.mp2 -i sample2-5secs.mp2"
                + " -filter_complex \"[0:a][1:a]concat=n=2:v=0:a=1\" 1-concat-out.mp2";
        String concatenateCommand = audioOperationService.generateConcatenateCommand(1, audioFileInfos);
        assertNotNull(concatenateCommand);
        assertEquals(expectedCommand, concatenateCommand);
    }

    @Test
    void shouldReturnConcatenatedAudioFileInfoWhenValidInputAudioFiles() {

        AudioFileInfo expectedAudio = new AudioFileInfo(
            Instant.parse("2023-04-28T09:00:00Z"),
            Instant.parse("2023-04-28T11:00:00Z"),
            "1-concat-out.mp2",
            1);

        when(audioTransformConfigurationProperties.getFfmpegExecutable()).thenReturn("/tempDir/ffmpeg");

        AudioFileInfo audioFileInfo =  audioOperationService.concatenate("/tempDir", audioFileInfos);
        assertEquals(expectedAudio.getFileName(), audioFileInfo.getFileName());
        assertEquals(expectedAudio.getChannel(), audioFileInfo.getChannel());
        assertEquals(expectedAudio.getStartTime(), audioFileInfo.getStartTime());
        assertEquals(expectedAudio.getEndTime(), audioFileInfo.getEndTime());
    }
}
