package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.impl.AudioOperationServiceImpl;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void processAudioForDownloadShouldReturnOneSessionWithOneAudioWhenProvidedWithOneAudio()
        throws ExecutionException, InterruptedException, IOException {

        AudioFileInfo trimmedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(trimmedAudioFileInfo);

        var mediaEntity = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity, SOME_DOWNLOAD_PATH);

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
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
    void processAudioForDownloadShouldReturnOneSessionWithOneAudioWhenProvidedWithTwoContinuousAudios()
        throws ExecutionException, InterruptedException, IOException {

        var concatenatedAudioFileInfo = new AudioFileInfo(TIME_12_00.toInstant(),
                                                          TIME_12_20.toInstant(),
                                                          "",
                                                          null);
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

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
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
    void processAudioForDownloadShouldReturnOneSessionWithTwoAudioWhenProvidedWithTwoNonContinuousAudiosWithDifferentChannelsAndSameTimestamp()
        throws ExecutionException, InterruptedException, IOException {

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

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
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
    void processAudioForDownloadShouldReturnTwoSessionsEachWithOneAudioWhenProvidedWithTwoNonContinuousAudios()
        throws ExecutionException, InterruptedException, IOException {

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

        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
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

    @Test
    void processAudioForPlaybackShouldPerformExpectedAudioOperations()
        throws ExecutionException, InterruptedException, IOException {
        var concatenatedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.concatenate(any(), any()))
            .thenReturn(concatenatedAudioFileInfo);

        AudioFileInfo mergedAudioFile = new AudioFileInfo(TIME_12_00.toInstant(),
                                                          TIME_12_20.toInstant(),
                                                          "",
                                                          1);
        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

        var trimmedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(trimmedAudioFileInfo);

        var reEncodedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.reEncode(any(), any()))
            .thenReturn(reEncodedAudioFileInfo);

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

        outboundFileProcessor.processAudioForPlayback(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        verify(audioOperationService, times(1)).concatenate(
            any(),
            any()
        );
        verify(audioOperationService, times(1)).merge(
            eq(Collections.singletonList(concatenatedAudioFileInfo)),
            any()
        );
        verify(audioOperationService, times(1)).trim(
            any(),
            eq(mergedAudioFile),
            eq(Duration.of(0, MINUTES)),
            eq(Duration.of(1, HOURS))
        );
        verify(audioOperationService, times(1)).reEncode(
            any(),
            eq(trimmedAudioFileInfo)
        );
    }

    @Test
    void processAudioShouldCallTrimWithExpectedArgumentsWhenDurationsIsPositive()
        throws ExecutionException, InterruptedException, IOException {
        var concatenatedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.concatenate(any(), any()))
            .thenReturn(concatenatedAudioFileInfo);

        AudioFileInfo mergedAudioFile = new AudioFileInfo(TIME_12_00.toInstant(),
                                                          TIME_12_20.toInstant(),
                                                          "",
                                                          1);
        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

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

        outboundFileProcessor.processAudioForPlayback(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        verify(audioOperationService, times(1)).trim(
            any(),
            eq(mergedAudioFile),
            eq(Duration.of(0, MINUTES)),
            eq(Duration.of(1, HOURS))
        );
    }

    @Test
    void processAudioShouldCallTrimWithExpectedArgumentsWhenDurationIsNegative()
        throws ExecutionException, InterruptedException, IOException {
        var concatenatedAudioFileInfo = new AudioFileInfo();
        when(audioOperationService.concatenate(any(), any()))
            .thenReturn(concatenatedAudioFileInfo);

        AudioFileInfo mergedAudioFile = new AudioFileInfo(TIME_12_10.toInstant(),
                                                          TIME_12_20.toInstant(),
                                                          "",
                                                          1);
        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

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

        outboundFileProcessor.processAudioForPlayback(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        verify(audioOperationService, times(1)).trim(
            any(),
            eq(mergedAudioFile),
            eq(Duration.of(-10, MINUTES)),
            eq(Duration.of(50, MINUTES))
        );
    }

    private MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        var mediaEntity = new MediaEntity();
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);

        return mediaEntity;
    }

}
