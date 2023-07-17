package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.impl.AudioOperationServiceImpl;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundFileProcessorImplTest {

    private static final Path SOME_DOWNLOAD_PATH = Path.of("/some-download-dir/some-downloaded-file");
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    private static final OffsetDateTime TIME_12_20 = OffsetDateTime.parse("2023-01-01T12:20Z");
    private static final OffsetDateTime TIME_12_30 = OffsetDateTime.parse("2023-01-01T12:30Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");

    private OutboundFileProcessorImpl outboundFileProcessor;

    @Mock
    private AudioOperationServiceImpl audioOperationService;

    @BeforeEach
    void setUp() {
        outboundFileProcessor = new OutboundFileProcessorImpl(audioOperationService);
    }

    @Test
    void processAudioShouldReturnOneSessionWithOneAudioWhenProvidedWithOneAudio()
        throws ExecutionException, InterruptedException {

        AudioFileInfo trimmedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(trimmedAudioFileInfo);

        var mediaEntity = createMediaEntity(TIME_12_00,
                                            TIME_12_10,
                                            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity, SOME_DOWNLOAD_PATH);

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudio(
            mediaEntityToDownloadLocation,
            TIME_12_00,
            TIME_13_00
        );

        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);

        assertEquals(1, session.size());
        assertEquals(trimmedAudioFileInfo, session.get(0));

        verify(audioOperationService, never()).concatenate(any(), any());
        verify(audioOperationService, times(1)).trim(any(), any(), any(), any());
    }

    @Test
    void processAudioShouldReturnOneSessionWithOneAudioWhenProvidedWithTwoContinuousAudios()
        throws ExecutionException, InterruptedException {

        var concatenatedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.concatenate(any(), any()))
            .thenReturn(concatenatedAudioFileInfo);

        var trimmedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(trimmedAudioFileInfo);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_20,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudio(
            mediaEntityToDownloadLocation,
            TIME_12_00,
            TIME_13_00
        );

        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);

        assertEquals(1, session.size());
        assertEquals(trimmedAudioFileInfo, session.get(0));

        verify(audioOperationService, times(1)).concatenate(any(), any());
        verify(audioOperationService, times(1)).trim(any(), any(), any(), any());
    }

    @Test
    void processAudioShouldReturnOneSessionWithTwoAudioWhenProvidedWithTwoNonContinuousAudiosWithDifferentChannelsAndSameTimestamp()
        throws ExecutionException, InterruptedException {

        var firstTrimmedAudioFileInfo = new AudioFileInfo();
        var secondTrimmedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            2
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudio(
            mediaEntityToDownloadLocation,
            TIME_12_00,
            TIME_13_00
        );

        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);

        assertEquals(2, session.size());
        assertEquals(firstTrimmedAudioFileInfo, session.get(0));
        assertEquals(secondTrimmedAudioFileInfo, session.get(1));

        verify(audioOperationService, never()).concatenate(any(), any());
        verify(audioOperationService, times(2)).trim(any(), any(), any(), any());
    }

    @Test
    void processAudioShouldReturnTwoSessionsEachWithOneAudioWhenProvidedWithTwoNonContinuousAudios()
        throws ExecutionException, InterruptedException {

        var firstTrimmedAudioFileInfo = new AudioFileInfo();
        var secondTrimmedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_20,
            TIME_12_30,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudio(
            mediaEntityToDownloadLocation,
            TIME_12_00,
            TIME_13_00
        );

        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        List<AudioFileInfo> secondSession = sessions.get(1);

        assertEquals(1, firstSession.size());
        assertEquals(firstTrimmedAudioFileInfo, firstSession.get(0));

        assertEquals(1, secondSession.size());
        assertEquals(secondTrimmedAudioFileInfo, secondSession.get(0));

        verify(audioOperationService, never()).concatenate(any(), any());
        verify(audioOperationService, times(2)).trim(any(), any(), any(), any());
    }

    private MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        var mediaEntity = new MediaEntity();
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);

        return mediaEntity;
    }

}
