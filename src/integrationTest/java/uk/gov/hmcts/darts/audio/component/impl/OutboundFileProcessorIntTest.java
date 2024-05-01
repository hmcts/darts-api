package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class OutboundFileProcessorIntTest extends IntegrationBase {
    private static final String AUDIO_FILENAME = "tests/audio/WithViqHeader/viq0001min.mp2";
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
    private Path tempDirectory;
    private Path audioPath;

    private HearingEntity hearingEntity;
    private UserAccountEntity testUser;

    @MockBean
    private UserIdentity mockUserIdentity;


    @Autowired
    private OutboundFileProcessorImpl outboundFileProcessor;

    @BeforeEach
    void setUp() throws IOException {
        UUID externalLocation = UUID.randomUUID();
        tempDirectory = Files.createTempDirectory(externalLocation + "darts_api_unit_test");

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME);
        audioPath = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test.mp2"), REPLACE_EXISTING);

        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            LocalDateTime.parse(HEARING_DATETIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor("aProsecutor");
        courtCase.addDefendant("aDefendant");
        courtCase.addDefence("aDefence");
        dartsDatabase.save(courtCase);

        testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity, audioPath);

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_09
        );

        // Then
        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);
        assertEquals(1, session.size());

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(audioPath)
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_10.toInstant())
            .endTime(TIME_12_19.toInstant())
            .channel(1)
            .mediaFile("0002.a00")
            .path(audioPath)
            .isTrimmed(true)
            .build();


        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_19
        );

        // Then
        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        List<AudioFileInfo> secondSession = sessions.get(1);

        assertEquals(1, firstSession.size());
        assertEquals(firstTrimmedAudioFileInfo, firstSession.get(0));

        assertEquals(1, secondSession.size());
        assertEquals(secondTrimmedAudioFileInfo, secondSession.get(0));

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_09.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(audioPath)
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_09.toInstant())
            .channel(2)
            .mediaFile("0001.a01")
            .path(audioPath)
            .isTrimmed(true)
            .build();


        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_09
        );

        // Then
        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);

        assertEquals(2, session.size());
        assertEquals(firstTrimmedAudioFileInfo, session.get(0));
        assertEquals(secondTrimmedAudioFileInfo, session.get(1));

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        var firstTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_02.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(audioPath)
            .isTrimmed(true)
            .build();
        var secondTrimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_20.toInstant())
            .endTime(TIME_12_29.toInstant())
            .channel(1)
            .mediaFile("0002.a00")
            .path(audioPath)
            .isTrimmed(true)
            .build();

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_29
        );

        // Then
        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        List<AudioFileInfo> secondSession = sessions.get(1);

        assertEquals(1, firstSession.size());
        assertEquals(firstTrimmedAudioFileInfo, firstSession.get(0));

        assertEquals(1, secondSession.size());
        assertEquals(secondTrimmedAudioFileInfo, secondSession.get(0));

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
        mediaEntityToDownloadLocation.put(mediaEntity1, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity2, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity3, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity4, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity5, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity6, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity7, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity8, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity9, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity10, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity11, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity12, audioPath);

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_10_01,
            TIME_12_50
        );

        // Then
        assertEquals(3, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        assertEquals(4, firstSession.size());

        List<AudioFileInfo> secondSession = sessions.get(1);
        assertEquals(4, secondSession.size());
        var session2UntrimmedAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_11_59.toInstant())
            .endTime(TIME_12_30.toInstant())
            .path(audioPath)
            .isTrimmed(false);
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(1)
                         .mediaFile("0002.a00")
                         .build(),
                     secondSession.get(0));
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

    }

    @Test
    void processAudioForPlaybackShouldPerformExpectedAudioOperations()
        throws ExecutionException, InterruptedException, IOException {

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);
    }

    @Test
    void processAudioForPlaybackMergeFails() {
        // given

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        // when then
        var exception = assertThrows(DartsApiException.class, () ->
            outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00));

        assertEquals("Failed to process audio request. No media present to process", exception.getMessage());
    }

    @Test
    void processAudioForPlaybackWithOneAudio() throws IOException, ExecutionException, InterruptedException {

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath);

        outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_02, TIME_12_10);

    }

    @Test
    void processAudioShouldCallTrimWithExpectedArgumentsWhenDurationsIsPositive()
        throws ExecutionException, InterruptedException, IOException {

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        // now that we have potentially multiple playback files - actual start/end of each segment is used
        // and there are no negative durations

    }

    @Test
    void processAudioForPlaybackShouldReturnThreeSessionsWithDifferentNumbersOfAudioWhenProvidedAudiosWithDiscrepanciesInAudioCounts()
        throws ExecutionException, InterruptedException, IOException {

        var reEncodedAudioFileInfo1 = AudioFileInfo.builder()
            .isTrimmed(true)
            .build();
        var reEncodedAudioFileInfo2 = AudioFileInfo.builder()
            .isTrimmed(true)
            .build();

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
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath,
                                                   mediaEntity3, audioPath,
                                                   mediaEntity4, audioPath,
                                                   mediaEntity5, audioPath
        );

        List<AudioFileInfo> sessions = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        assertEquals(3, sessions.size());
        AudioFileInfo firstSession = sessions.get(0);
        AudioFileInfo secondSession = sessions.get(1);

        assertEquals(reEncodedAudioFileInfo1, firstSession);
        assertEquals(reEncodedAudioFileInfo2, secondSession);

    }

    @Test
    void processAudioForPlaybackShouldReturnTwoSessionsWhenProvidedAudiosWithVaryingStartEndTimesOnSameChannel()
        throws ExecutionException, InterruptedException, IOException {

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

        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        List<AudioFileInfo> sessions = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_10_01, TIME_11_59);

        assertEquals(2, sessions.size());
        AudioFileInfo firstSession = sessions.get(0);
        AudioFileInfo secondSession = sessions.get(1);

        assertEquals(TIME_10_01.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_11_00.toInstant(), firstSession.getEndTime());
        assertEquals(TIME_11_01.toInstant(), secondSession.getStartTime());
        assertEquals(TIME_11_59.toInstant(), secondSession.getEndTime());

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


    private Path createFile(Path path, String name) throws IOException {
        return Files.createFile(path.resolve(name));
    }
}
