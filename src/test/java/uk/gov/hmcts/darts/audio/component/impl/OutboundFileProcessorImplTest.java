package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.impl.AudioOperationServiceImpl;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundFileProcessorImplTest {

    private static final Path SOME_DOWNLOAD_PATH = Path.of("/some-download-dir/some-downloaded-file");
    private static final OffsetDateTime TIME_10_00 = OffsetDateTime.parse("2023-01-01T10:00Z");
    private static final OffsetDateTime TIME_10_01 = OffsetDateTime.parse("2023-01-01T10:01Z");
    private static final OffsetDateTime TIME_11_00 = OffsetDateTime.parse("2023-01-01T11:00Z");
    private static final OffsetDateTime TIME_11_01 = OffsetDateTime.parse("2023-01-01T11:01Z");
    private static final OffsetDateTime TIME_11_59 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_02 = OffsetDateTime.parse("2023-01-01T12:02:00Z");
    private static final OffsetDateTime TIME_12_09 = OffsetDateTime.parse("2023-01-01T12:09:00Z");
    private static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    private static final OffsetDateTime TIME_12_19 = OffsetDateTime.parse("2023-01-01T12:19:00Z");
    private static final OffsetDateTime TIME_12_20 = OffsetDateTime.parse("2023-01-01T12:20Z");
    private static final OffsetDateTime TIME_12_29 = OffsetDateTime.parse("2023-01-01T12:29Z");
    private static final OffsetDateTime TIME_12_30 = OffsetDateTime.parse("2023-01-01T12:30Z");
    private static final OffsetDateTime TIME_12_40 = OffsetDateTime.parse("2023-01-01T12:40Z");
    private static final OffsetDateTime TIME_12_50 = OffsetDateTime.parse("2023-01-01T12:50Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");

    private OutboundFileProcessorImpl outboundFileProcessor;

    @Mock
    private AudioOperationServiceImpl audioOperationService;
    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @BeforeEach
    void setUp() {
        audioConfigurationProperties.setAllowableAudioGapDuration(Duration.ofSeconds(1));
        outboundFileProcessor = new OutboundFileProcessorImpl(audioOperationService, audioConfigurationProperties);
    }

    @Test
    void processAudioForDownloadShouldReturnOneSessionWithOneAudioWhenProvidedWithOneAudio()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity, SOME_DOWNLOAD_PATH);

        AudioFileInfo originalAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_00.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(false)
            .build();

        AudioFileInfo trimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_09.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();

        when(audioOperationService.trim(
            "",
            originalAudioFileInfo,
            Duration.ofMinutes(2),
            Duration.ofMinutes(9)))
            .thenReturn(trimmedAudioFileInfo);

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_09
        );

        // Then
        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.getFirst();

        assertEquals(1, session.size());
        assertEquals(trimmedAudioFileInfo, session.getFirst());

        verify(audioOperationService).trim(
            "",
            originalAudioFileInfo,
            Duration.ofMinutes(2),
            Duration.ofMinutes(9));
        verifyNoMoreInteractions(audioOperationService);
    }

    @Test
    void processAudioForDownloadShouldReturnTwoSessionsEachWithOneAudioWhenProvidedWithTwoContinuousAudios()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_20,
            2,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_10.toInstant())
            .endTime(TIME_12_19.toInstant())
            .channel(1)
            .mediaFile("0002.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_19
        );

        // Then
        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.getFirst();
        List<AudioFileInfo> secondSession = sessions.get(1);

        assertEquals(1, firstSession.size());
        assertEquals(firstTrimmedAudioFileInfo, firstSession.getFirst());

        assertEquals(1, secondSession.size());
        assertEquals(secondTrimmedAudioFileInfo, secondSession.getFirst());

        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_00.toInstant())
                .endTime(TIME_12_10.toInstant())
                .channel(1)
                .mediaFile("0001.a00")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10));
        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_10.toInstant())
                .endTime(TIME_12_20.toInstant())
                .channel(1)
                .mediaFile("0002.a00")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ZERO,
            Duration.ofMinutes(9));
        verifyNoMoreInteractions(audioOperationService);
    }

    @Test
    void processAudioForDownloadShouldReturnOneSessionWithTwoAudioWhenProvidedWithTwoNonContinuousAudiosWithDifferentChannelsAndSameTimestamp()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            2
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_09.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_09.toInstant())
            .channel(2)
            .mediaFile("0001.a01")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);


        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_09
        );

        // Then
        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.getFirst();

        assertEquals(2, session.size());
        assertEquals(firstTrimmedAudioFileInfo, session.getFirst());
        assertEquals(secondTrimmedAudioFileInfo, session.get(1));

        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_00.toInstant())
                .endTime(TIME_12_10.toInstant())
                .channel(1)
                .mediaFile("0001.a00")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ofMinutes(2),
            Duration.ofMinutes(9));
        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_00.toInstant())
                .endTime(TIME_12_10.toInstant())
                .channel(2)
                .mediaFile("0001.a01")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ofMinutes(2),
            Duration.ofMinutes(9));
        verifyNoMoreInteractions(audioOperationService);
    }

    @Test
    void processAudioForDownloadShouldReturnTwoSessionsEachWithOneAudioWhenProvidedWithTwoNonContinuousAudios()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_20,
            TIME_12_30,
            2,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_20.toInstant())
            .endTime(TIME_12_29.toInstant())
            .channel(1)
            .mediaFile("0002.a00")
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true)
            .build();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_29
        );

        // Then
        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.getFirst();
        List<AudioFileInfo> secondSession = sessions.get(1);

        assertEquals(1, firstSession.size());
        assertEquals(firstTrimmedAudioFileInfo, firstSession.getFirst());

        assertEquals(1, secondSession.size());
        assertEquals(secondTrimmedAudioFileInfo, secondSession.getFirst());

        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_00.toInstant())
                .endTime(TIME_12_10.toInstant())
                .channel(1)
                .mediaFile("0001.a00")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10));
        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_20.toInstant())
                .endTime(TIME_12_30.toInstant())
                .channel(1)
                .mediaFile("0002.a00")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ZERO,
            Duration.ofMinutes(9));
        verifyNoMoreInteractions(audioOperationService);
    }

    @Test
    void processAudioForDownloadShouldReturnThreeSessionsWithStartAndEndSessionsTrimmedOnly()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_10_00,
            TIME_11_00,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_10_00,
            TIME_11_00,
            1,
            2
        );
        var mediaEntity3 = createMediaEntity(
            TIME_10_00,
            TIME_11_00,
            1,
            3
        );
        var mediaEntity4 = createMediaEntity(
            TIME_10_00,
            TIME_11_00,
            1,
            4
        );
        var mediaEntity5 = createMediaEntity(
            TIME_11_59,
            TIME_12_30,
            2,
            1
        );
        var mediaEntity6 = createMediaEntity(
            TIME_11_59,
            TIME_12_30,
            2,
            2
        );
        var mediaEntity7 = createMediaEntity(
            TIME_11_59,
            TIME_12_30,
            2,
            3
        );
        var mediaEntity8 = createMediaEntity(
            TIME_11_59,
            TIME_12_30,
            2,
            4
        );
        var mediaEntity9 = createMediaEntity(
            TIME_12_30,
            TIME_13_00,
            3,
            1
        );
        var mediaEntity10 = createMediaEntity(
            TIME_12_30,
            TIME_13_00,
            3,
            2
        );
        var mediaEntity11 = createMediaEntity(
            TIME_12_30,
            TIME_13_00,
            3,
            3
        );
        var mediaEntity12 = createMediaEntity(
            TIME_12_30,
            TIME_13_00,
            3,
            4
        );

        var mediaEntityToDownloadLocation = new LinkedHashMap<MediaEntity, Path>();
        mediaEntityToDownloadLocation.put(mediaEntity1, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity2, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity3, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity4, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity5, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity6, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity7, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity8, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity9, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity10, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity11, SOME_DOWNLOAD_PATH);
        mediaEntityToDownloadLocation.put(mediaEntity12, SOME_DOWNLOAD_PATH);

        var session1TrimmedAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_10_01.toInstant())
            .endTime(TIME_11_00.toInstant())
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true);
        var session3TrimmedAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_12_30.toInstant())
            .endTime(TIME_12_50.toInstant())
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(true);
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(session1TrimmedAudioFileInfoBuilder
                            .channel(1)
                            .mediaFile("0001.a00")
                            .build())
            .thenReturn(session1TrimmedAudioFileInfoBuilder
                            .channel(2)
                            .mediaFile("0001.a01")
                            .build())
            .thenReturn(session1TrimmedAudioFileInfoBuilder
                            .channel(3)
                            .mediaFile("0001.a02")
                            .build())
            .thenReturn(session1TrimmedAudioFileInfoBuilder
                            .channel(4)
                            .mediaFile("0001.a03")
                            .build())
            .thenReturn(session3TrimmedAudioFileInfoBuilder
                            .channel(1)
                            .mediaFile("0003.a00")
                            .build())
            .thenReturn(session3TrimmedAudioFileInfoBuilder
                            .channel(2)
                            .mediaFile("0003.a01")
                            .build())
            .thenReturn(session3TrimmedAudioFileInfoBuilder
                            .channel(3)
                            .mediaFile("0003.a02")
                            .build())
            .thenReturn(session3TrimmedAudioFileInfoBuilder
                            .channel(4)
                            .mediaFile("0003.a03")
                            .build());

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_10_01,
            TIME_12_50
        );

        // Then
        assertEquals(3, sessions.size());
        List<AudioFileInfo> firstSession = sessions.getFirst();
        assertEquals(4, firstSession.size());

        List<AudioFileInfo> secondSession = sessions.get(1);
        assertEquals(4, secondSession.size());
        var session2UntrimmedAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_11_59.toInstant())
            .endTime(TIME_12_30.toInstant())
            .path(SOME_DOWNLOAD_PATH)
            .isTrimmed(false);
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(1)
                         .mediaFile("0002.a00")
                         .build(),
                     secondSession.getFirst());
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(2)
                         .mediaFile("0002.a01")
                         .build(),
                     secondSession.get(1));
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(3)
                         .mediaFile("0002.a02")
                         .build(),
                     secondSession.get(2));
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(4)
                         .mediaFile("0002.a03")
                         .build(),
                     secondSession.get(3));

        List<AudioFileInfo> thirdSession = sessions.get(2);
        assertEquals(4, thirdSession.size());

        var session1OriginalAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_10_00.toInstant())
            .endTime(TIME_11_00.toInstant())
            .path(SOME_DOWNLOAD_PATH);
        var session1ExpectedStartDuration = Duration.ofMinutes(1);
        var session1ExpectedEndDuration = Duration.ofHours(1);
        verify(audioOperationService).trim(
            "",
            session1OriginalAudioFileInfoBuilder
                .channel(1)
                .mediaFile("0001.a00")
                .build(),
            session1ExpectedStartDuration,
            session1ExpectedEndDuration);
        verify(audioOperationService).trim(
            "",
            session1OriginalAudioFileInfoBuilder
                .channel(2)
                .mediaFile("0001.a01")
                .build(),
            session1ExpectedStartDuration,
            session1ExpectedEndDuration);
        verify(audioOperationService).trim(
            "",
            session1OriginalAudioFileInfoBuilder
                .channel(3)
                .mediaFile("0001.a02")
                .build(),
            session1ExpectedStartDuration,
            session1ExpectedEndDuration);
        verify(audioOperationService).trim(
            "",
            session1OriginalAudioFileInfoBuilder
                .channel(4)
                .mediaFile("0001.a03")
                .build(),
            session1ExpectedStartDuration,
            session1ExpectedEndDuration);

        var session3OriginalAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_12_30.toInstant())
            .endTime(TIME_13_00.toInstant())
            .path(SOME_DOWNLOAD_PATH);
        var session3ExpectedStartDuration = Duration.ZERO;
        var session3ExpectedEndDuration = Duration.ofMinutes(20);
        verify(audioOperationService).trim(
            "",
            session3OriginalAudioFileInfoBuilder
                .channel(1)
                .mediaFile("0003.a00")
                .build(),
            session3ExpectedStartDuration,
            session3ExpectedEndDuration);
        verify(audioOperationService).trim(
            "",
            session3OriginalAudioFileInfoBuilder
                .channel(2)
                .mediaFile("0003.a01")
                .build(),
            session3ExpectedStartDuration,
            session3ExpectedEndDuration);
        verify(audioOperationService).trim(
            "",
            session3OriginalAudioFileInfoBuilder
                .channel(3)
                .mediaFile("0003.a02")
                .build(),
            session3ExpectedStartDuration,
            session3ExpectedEndDuration);
        verify(audioOperationService).trim(
            "",
            session3OriginalAudioFileInfoBuilder
                .channel(4)
                .mediaFile("0003.a03")
                .build(),
            session3ExpectedStartDuration,
            session3ExpectedEndDuration);
        verifyNoMoreInteractions(audioOperationService);
    }

    @Test
    void processAudioForPlaybackShouldPerformExpectedAudioOperations()
        throws ExecutionException, InterruptedException, IOException {
        AudioFileInfo concatenatedAudioFileInfo = AudioFileInfo.builder().build();

        List<AudioFileInfo> concatenatedAudioFileInfoList = List.of(
            concatenatedAudioFileInfo
        );
        when(audioOperationService.concatenateWithGaps(any(), any(), any()))
            .thenReturn(concatenatedAudioFileInfoList);

        AudioFileInfo mergedAudioFile = AudioFileInfo.builder()
            .startTime(TIME_12_00.toInstant())
            .endTime(TIME_12_20.toInstant())
            .channel(0)
            .path(SOME_DOWNLOAD_PATH)
            .build();

        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

        var trimmedAudioFileInfo = AudioFileInfo.builder().build();
        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(trimmedAudioFileInfo);

        var reEncodedAudioFileInfo = AudioFileInfo.builder().build();
        when(audioOperationService.reEncode(any(), any()))
            .thenReturn(reEncodedAudioFileInfo);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_20,
            2,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        verify(audioOperationService, times(1)).concatenateWithGaps(
            any(),
            any(),
            any()
        );
        verify(audioOperationService, times(1)).merge(
            eq(concatenatedAudioFileInfoList),
            any()
        );
        // now that we have potentially multiple playback files - actual start/end of each segment is used
        // and there are no negative durations
        verify(audioOperationService, times(1)).trim(
            any(),
            eq(mergedAudioFile),
            eq(Duration.of(0, MINUTES)),
            eq(Duration.of(20, MINUTES))
        );
        verify(audioOperationService, times(1)).reEncode(
            any(),
            eq(trimmedAudioFileInfo)
        );
    }

    @Test
    void processAudioForPlaybackMergeFails()
        throws ExecutionException, InterruptedException, IOException {
        // given
        AudioFileInfo concatenatedAudioFileInfo = AudioFileInfo.builder().build();

        List<AudioFileInfo> concatenatedAudioFileInfoList = List.of(
            concatenatedAudioFileInfo
        );
        when(audioOperationService.concatenateWithGaps(any(), any(), any()))
            .thenReturn(concatenatedAudioFileInfoList);

        AudioFileInfo mergedAudioFile = null;

        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_20,
            2,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        // when then
        var exception = assertThrows(DartsApiException.class, () ->
            outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00));

        assertEquals("Failed to process audio request. No media present to process", exception.getMessage());
    }

    @Test
    void processAudioForPlaybackWithOneAudio()
        throws ExecutionException, InterruptedException, IOException {

        AudioFileInfo mergedAudioFile = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .path(SOME_DOWNLOAD_PATH)
            .mediaFile("0001.a00")
            .build();

        when(audioOperationService.merge(any(), any())).thenReturn(mergedAudioFile);

        var trimmedAudioFileInfo = AudioFileInfo.builder()
            .isTrimmed(true)
            .mediaFile(mergedAudioFile.getMediaFile())
            .channel(mergedAudioFile.getChannel())
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_10.toInstant())
            .path(mergedAudioFile.getPath())
            .build();
        when(audioOperationService.trim(any(), any(), any(), any())).thenReturn(trimmedAudioFileInfo);

        var reEncodedAudioFileInfo = AudioFileInfo.builder().build();
        when(audioOperationService.reEncode(any(), any())).thenReturn(reEncodedAudioFileInfo);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH);

        outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_02, TIME_12_10);

        verify(audioOperationService).merge(any(), any());

        verify(audioOperationService).trim(
            "",
            AudioFileInfo.builder()
                .startTime(TIME_12_02.toInstant())
                .endTime(TIME_12_10.toInstant())
                .channel(1)
                .mediaFile("0001.a00")
                .path(SOME_DOWNLOAD_PATH)
                .build(),
            Duration.ofSeconds(0),
            Duration.ofMinutes(8));

        verify(audioOperationService, times(1)).reEncode(any(), eq(trimmedAudioFileInfo));

    }

    @Test
    void processAudioForPlaybackShouldCallTrimWithExpectedArgumentsWhenDurationsIsPositive()
        throws ExecutionException, InterruptedException, IOException {
        AudioFileInfo concatenatedAudioFileInfo = AudioFileInfo.builder().build();
        List<AudioFileInfo> concatenatedAudioFileInfoList = List.of(
            concatenatedAudioFileInfo
        );
        when(audioOperationService.concatenateWithGaps(any(), any(), any()))
            .thenReturn(concatenatedAudioFileInfoList);

        AudioFileInfo mergedAudioFile = AudioFileInfo.builder()
            .startTime(TIME_12_00.toInstant())
            .endTime(TIME_12_20.toInstant())
            .channel(0)
            .path(SOME_DOWNLOAD_PATH)
            .build();

        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_20,
            2,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        // now that we have potentially multiple playback files - actual start/end of each segment is used
        // and there are no negative durations
        verify(audioOperationService, times(1)).trim(
            any(),
            eq(mergedAudioFile),
            eq(Duration.of(0, MINUTES)),
            eq(Duration.of(20, MINUTES))
        );
    }

    @Test
    void processAudioForPlaybackShouldReturnThreeSessionsWithDifferentNumbersOfAudioWhenProvidedAudiosWithDiscrepanciesInAudioCounts()
        throws ExecutionException, InterruptedException, IOException {

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .isTrimmed(true)
            .build();

        AudioFileInfo mergedAudioFile = AudioFileInfo.builder()
            .startTime(TIME_12_00.toInstant())
            .endTime(TIME_12_20.toInstant())
            .channel(0)
            .path(SOME_DOWNLOAD_PATH)
            .build();

        var reEncodedAudioFileInfo1 = AudioFileInfo.builder()
            .isTrimmed(true)
            .build();
        var reEncodedAudioFileInfo2 = AudioFileInfo.builder()
            .isTrimmed(true)
            .build();
        when(audioOperationService.reEncode(any(), any()))
            .thenReturn(reEncodedAudioFileInfo1)
            .thenReturn(reEncodedAudioFileInfo2);

        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile);

        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            2
        );
        var mediaEntity3 = createMediaEntity(
            TIME_12_20,
            TIME_12_30,
            2,
            1
        );
        var mediaEntity4 = createMediaEntity(
            TIME_12_20,
            TIME_12_30,
            2,
            2
        );
        var mediaEntity5 = createMediaEntity(
            TIME_12_40,
            TIME_12_50,
            3,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH,
                                                   mediaEntity3, SOME_DOWNLOAD_PATH,
                                                   mediaEntity4, SOME_DOWNLOAD_PATH,
                                                   mediaEntity5, SOME_DOWNLOAD_PATH
        );

        List<AudioFileInfo> sessions = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        assertEquals(3, sessions.size());
        AudioFileInfo firstSession = sessions.getFirst();
        AudioFileInfo secondSession = sessions.get(1);

        assertEquals(reEncodedAudioFileInfo1, firstSession);
        assertEquals(reEncodedAudioFileInfo2, secondSession);

        verify(audioOperationService, never()).concatenate(any(), any());
        verify(audioOperationService, times(3)).trim(any(), any(), any(), any());
    }

    @Test
    void processAudioForPlaybackShouldReturnTwoSessionsWhenProvidedAudiosWithVaryingStartEndTimesOnSameChannel()
        throws ExecutionException, InterruptedException, IOException {

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_10_01.toInstant())
            .endTime(TIME_11_00.toInstant())
            .channel(1)
            .mediaFile("audio1")
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_11_01.toInstant())
            .endTime(TIME_11_59.toInstant())
            .channel(1)
            .mediaFile("audio2")
            .isTrimmed(true)
            .build();

        AudioFileInfo mergedAudioFile1 = AudioFileInfo.builder()
            .startTime(TIME_10_00.toInstant())
            .endTime(TIME_11_00.toInstant())
            .channel(1)
            .mediaFile("merged-audio1")
            .build();

        AudioFileInfo mergedAudioFile2 = AudioFileInfo.builder()
            .startTime(TIME_11_00.toInstant())
            .endTime(TIME_12_00.toInstant())
            .channel(1)
            .mediaFile("merged-audio2")
            .build();

        when(audioOperationService.reEncode(any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        when(audioOperationService.merge(any(), any()))
            .thenReturn(mergedAudioFile1)
            .thenReturn(mergedAudioFile2);

        when(audioOperationService.trim(any(), any(), any(), any()))
            .thenReturn(firstTrimmedAudioFileInfo)
            .thenReturn(secondTrimmedAudioFileInfo);

        List<AudioFileInfo> concatenatedAudioFileInfoList = new ArrayList<>(Arrays.asList(mergedAudioFile1, mergedAudioFile2));
        when(audioOperationService.concatenateWithGaps(any(), any(), any()))
            .thenReturn(concatenatedAudioFileInfoList);

        var mediaEntity1 = createMediaEntity(
            TIME_10_00,
            TIME_11_00,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_11_01,
            TIME_12_00,
            2,
            1
        );

        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, SOME_DOWNLOAD_PATH,
                                                   mediaEntity2, SOME_DOWNLOAD_PATH
        );

        List<AudioFileInfo> sessions = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_10_01, TIME_11_59);

        assertEquals(2, sessions.size());
        AudioFileInfo firstSession = sessions.getFirst();
        AudioFileInfo secondSession = sessions.get(1);

        assertEquals(TIME_10_01.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_11_00.toInstant(), firstSession.getEndTime());
        assertEquals(TIME_11_01.toInstant(), secondSession.getStartTime());
        assertEquals(TIME_11_59.toInstant(), secondSession.getEndTime());

        verify(audioOperationService, never()).concatenate(any(), any());
        verify(audioOperationService, times(2)).trim(any(), any(), any(), any());
    }

    private MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int session, int channel) {
        var mediaEntity = new MediaEntity();
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);
        mediaEntity.setMediaFile(String.format("000%d.a0%d", session, channel - 1));
        mediaEntity.setMediaFormat("mpeg2");
        mediaEntity.setFileSize(240_744L);
        mediaEntity.setChecksum("wysXTgRikGN6nMB8AJ0JrQ==");
        mediaEntity.setMediaType('A');

        return mediaEntity;
    }

}
