package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

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

@Disabled("Impacted by V1_366__add_missing_constraints_part5b.sql")
@TestPropertySource(properties = {"darts.audio.transformation.service.audio.file=tests/audio/WithViqHeader/viq0001min.mp2"})
@Slf4j
@Disabled("Impacted by V1_363__not_null_constraints_part3.sql")
class OutboundFileProcessorIntTest extends IntegrationBase {
    private static final String AUDIO_FILENAME = "tests/audio/WithViqHeader/viq0001min.mp2";
    private static final String HEARING_DATETIME = "2023-01-01T10:00:00";
    private static final OffsetDateTime TIME_10_00 = OffsetDateTime.parse("2023-01-01T10:00:00Z");
    private static final OffsetDateTime TIME_10_00_15 = OffsetDateTime.parse("2023-01-01T10:00:15Z");
    private static final OffsetDateTime TIME_10_00_30 = OffsetDateTime.parse("2023-01-01T10:00:30Z");
    private static final OffsetDateTime TIME_10_00_45 = OffsetDateTime.parse("2023-01-01T10:00:45Z");
    private static final OffsetDateTime TIME_10_01 = OffsetDateTime.parse("2023-01-01T10:01Z");
    private static final OffsetDateTime TIME_10_02 = OffsetDateTime.parse("2023-01-01T10:02Z");
    private static final OffsetDateTime TIME_10_03 = OffsetDateTime.parse("2023-01-01T10:03Z");
    private static final OffsetDateTime TIME_11_59 = OffsetDateTime.parse("2023-01-01T11:59Z");
    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime TIME_12_00_15 = OffsetDateTime.parse("2023-01-01T12:00:15Z");
    private static final OffsetDateTime TIME_12_01 = OffsetDateTime.parse("2023-01-01T12:01:00Z");
    private static final OffsetDateTime TIME_12_02 = OffsetDateTime.parse("2023-01-01T12:02:00Z");
    private static final OffsetDateTime TIME_12_09 = OffsetDateTime.parse("2023-01-01T12:09:00Z");
    private static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10:00Z");
    private static final OffsetDateTime TIME_12_11 = OffsetDateTime.parse("2023-01-01T12:11:00Z");
    private static final OffsetDateTime TIME_12_19 = OffsetDateTime.parse("2023-01-01T12:19:00Z");
    private static final OffsetDateTime TIME_12_20 = OffsetDateTime.parse("2023-01-01T12:20Z");
    private static final OffsetDateTime TIME_12_29 = OffsetDateTime.parse("2023-01-01T12:29Z");
    private static final OffsetDateTime TIME_12_29_45 = OffsetDateTime.parse("2023-01-01T12:45Z");
    private static final OffsetDateTime TIME_12_30 = OffsetDateTime.parse("2023-01-01T12:30Z");
    private static final OffsetDateTime TIME_12_30_30 = OffsetDateTime.parse("2023-01-01T12:30:30Z");
    private static final OffsetDateTime TIME_12_31 = OffsetDateTime.parse("2023-01-01T12:31Z");
    private static final OffsetDateTime TIME_12_50 = OffsetDateTime.parse("2023-01-01T12:50Z");
    private static final OffsetDateTime TIME_12_51 = OffsetDateTime.parse("2023-01-01T12:51Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";
    private Path tempDirectory;
    private Path audioPath;

    @Autowired
    private OutboundFileProcessorImpl outboundFileProcessor;

    @BeforeEach
    void setUp() throws IOException {
        UUID externalLocation = UUID.randomUUID();
        tempDirectory = Files.createTempDirectory(externalLocation + "darts_api_unit_test");

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME);
        audioPath = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test.mp2"), REPLACE_EXISTING);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
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

    }

    @Test
    void processAudioForDownloadShouldReturnOneSessionWithOneAudioWhenProvidedWithOneAudio()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity = createMediaEntity(
            TIME_12_09,
            TIME_12_10,
            1,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity, audioPath);

        AudioFileInfo trimmedAudioFileInfo = AudioFileInfo.builder()
            .startTime(TIME_12_09.toInstant())
            .endTime(TIME_12_10.toInstant())
            .channel(1)
            .mediaFile("0001.a00")
            .path(audioPath)
            .isTrimmed(false)
            .build();

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_02,
            TIME_12_10
        );

        // Then
        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);
        assertEquals(1, session.size());

        assertEquals(1, session.size());
        assertEquals(trimmedAudioFileInfo, session.get(0));
    }

    @Test
    void processAudioForDownloadShouldReturnTwoSessionsEachWithOneAudioWhenProvidedWithTwoContinuousAudios()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_12_09,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_11,
            2,
            1
        );
        File audioFileTest2 = TestUtils.getFile(AUDIO_FILENAME);
        Path audioPath2 = Files.copy(audioFileTest2.toPath(), createFile(tempDirectory, "audio-test2.mp2"), REPLACE_EXISTING);

        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath2
        );

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_09,
            TIME_12_11
        );

        // Then
        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        List<AudioFileInfo> secondSession = sessions.get(1);
        assertEquals(1, firstSession.size());
        assertEquals(1, secondSession.size());
    }

    @Test
    void processAudioForDownloadShouldReturnOneSessionWithTwoAudioWhenProvidedWithTwoNonContinuousAudiosWithDifferentChannelsAndSameTimestamp()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_11_59,
            TIME_12_00,
            1,
            1
        );

        var mediaEntity2 = createMediaEntity(
            TIME_11_59,
            TIME_12_00,
            1,
            2
        );
        File audioFileTest2 = TestUtils.getFile(AUDIO_FILENAME);
        Path audioPath2 = Files.copy(audioFileTest2.toPath(), createFile(tempDirectory, "audio-test2.mp2"), REPLACE_EXISTING);

        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath2
        );

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_11_59,
            TIME_12_00
        );

        // Then
        assertEquals(1, sessions.size());
        List<AudioFileInfo> session = sessions.get(0);

        assertEquals(2, session.size());

    }

    @Test
    void processAudioForDownloadShouldReturnTwoSessionsEachWithOneAudioWhenProvidedWithTwoNonContinuousAudios()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_01,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_29,
            TIME_12_30,
            2,
            1
        );
        File audioFileTest2 = TestUtils.getFile(AUDIO_FILENAME);
        Path audioPath2 = Files.copy(audioFileTest2.toPath(), createFile(tempDirectory, "audio-test2.mp2"), REPLACE_EXISTING);

        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath2
        );

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_12_00_15,
            TIME_12_29_45
        );

        // Then
        assertEquals(2, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        List<AudioFileInfo> secondSession = sessions.get(1);

        assertEquals(1, firstSession.size());

        assertEquals(1, secondSession.size());

    }

    @Test
    void processAudioForDownloadShouldReturnThreeSessionsWithStartAndEndSessionsTrimmedOnly()
        throws ExecutionException, InterruptedException, IOException {

        // Given
        var mediaEntity1 = createMediaEntity(
            TIME_10_00,
            TIME_10_01,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_10_00,
            TIME_10_01,
            1,
            2
        );
        var mediaEntity3 = createMediaEntity(
            TIME_10_00,
            TIME_10_01,
            1,
            3
        );
        var mediaEntity4 = createMediaEntity(
            TIME_10_00,
            TIME_10_01,
            1,
            4
        );
        var mediaEntity5 = createMediaEntity(
            TIME_11_59,
            TIME_12_00,
            2,
            1
        );
        var mediaEntity6 = createMediaEntity(
            TIME_11_59,
            TIME_12_00,
            2,
            2
        );
        var mediaEntity7 = createMediaEntity(
            TIME_11_59,
            TIME_12_00,
            2,
            3
        );
        var mediaEntity8 = createMediaEntity(
            TIME_11_59,
            TIME_12_00,
            2,
            4
        );
        var mediaEntity9 = createMediaEntity(
            TIME_12_30,
            TIME_12_31,
            3,
            1
        );
        var mediaEntity10 = createMediaEntity(
            TIME_12_30,
            TIME_12_31,
            3,
            2
        );
        var mediaEntity11 = createMediaEntity(
            TIME_12_30,
            TIME_12_31,
            3,
            3
        );
        var mediaEntity12 = createMediaEntity(
            TIME_12_30,
            TIME_12_31,
            3,
            4
        );

        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME);
        Path audioPath2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test2.mp2"), REPLACE_EXISTING);
        Path audioPath3 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test3.mp2"), REPLACE_EXISTING);
        Path audioPath4 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test4.mp2"), REPLACE_EXISTING);
        Path audioPath5 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test5.mp2"), REPLACE_EXISTING);
        Path audioPath6 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test6.mp2"), REPLACE_EXISTING);
        Path audioPath7 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test7.mp2"), REPLACE_EXISTING);
        Path audioPath8 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test8.mp2"), REPLACE_EXISTING);
        Path audioPath9 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test9.mp2"), REPLACE_EXISTING);
        Path audioPath10 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test10.mp2"), REPLACE_EXISTING);
        Path audioPath11 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test11.mp2"), REPLACE_EXISTING);
        Path audioPath12 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test12.mp2"), REPLACE_EXISTING);

        Map<MediaEntity, Path> mediaEntityToDownloadLocation = new LinkedHashMap<>();
        mediaEntityToDownloadLocation.put(mediaEntity1, audioPath);
        mediaEntityToDownloadLocation.put(mediaEntity2, audioPath2);
        mediaEntityToDownloadLocation.put(mediaEntity3, audioPath3);
        mediaEntityToDownloadLocation.put(mediaEntity4, audioPath4);
        mediaEntityToDownloadLocation.put(mediaEntity5, audioPath5);
        mediaEntityToDownloadLocation.put(mediaEntity6, audioPath6);
        mediaEntityToDownloadLocation.put(mediaEntity7, audioPath7);
        mediaEntityToDownloadLocation.put(mediaEntity8, audioPath8);
        mediaEntityToDownloadLocation.put(mediaEntity9, audioPath9);
        mediaEntityToDownloadLocation.put(mediaEntity10, audioPath10);
        mediaEntityToDownloadLocation.put(mediaEntity11, audioPath11);
        mediaEntityToDownloadLocation.put(mediaEntity12, audioPath12);

        // When
        List<List<AudioFileInfo>> sessions = outboundFileProcessor.processAudioForDownload(
            mediaEntityToDownloadLocation,
            TIME_10_00_30,
            TIME_12_30_30
        );

        // Then
        assertEquals(3, sessions.size());
        List<AudioFileInfo> firstSession = sessions.get(0);
        assertEquals(4, firstSession.size());

        List<AudioFileInfo> secondSession = sessions.get(1);
        assertEquals(4, secondSession.size());
        var session2UntrimmedAudioFileInfoBuilder = AudioFileInfo.builder()
            .startTime(TIME_11_59.toInstant())
            .endTime(TIME_12_00.toInstant())
            .isTrimmed(false);

        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(1)
                         .mediaFile("0002.a00")
                         .path(audioPath5)
                         .build(),
                     secondSession.get(0));
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(2)
                         .mediaFile("0002.a01")
                         .path(audioPath6)
                         .build(),
                     secondSession.get(1));
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(3)
                         .mediaFile("0002.a02")
                         .path(audioPath7)
                         .build(),
                     secondSession.get(2));
        assertEquals(session2UntrimmedAudioFileInfoBuilder
                         .channel(4)
                         .mediaFile("0002.a03")
                         .path(audioPath8)
                         .build(),
                     secondSession.get(3));

        List<AudioFileInfo> thirdSession = sessions.get(2);
        assertEquals(4, thirdSession.size());

    }

    @Test
    void processAudioForPlaybackShouldReturnOneSessionWithOneAudio()
        throws ExecutionException, InterruptedException, IOException {
        // given
        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_01,
            1,
            1
        );

        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath);

        // when
        List<AudioFileInfo> audioResults = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_11_59, TIME_13_00);

        // then
        assertEquals(1, audioResults.size());
        AudioFileInfo firstSession = audioResults.get(0);

        assertEquals(TIME_12_00.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_12_01.toInstant(), firstSession.getEndTime());
    }

    @Test
    void processAudioForPlaybackShouldReturnOneSessionWithTwoContinuousAudio() throws IOException, ExecutionException, InterruptedException {
        // given

        var mediaEntity1 = createMediaEntity(
            TIME_12_00,
            TIME_12_01,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_01,
            TIME_12_02,
            2,
            1
        );
        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME);
        Path audioPath2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test2.mp2"), REPLACE_EXISTING);
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath2
        );

        // when
        List<AudioFileInfo> audioResults = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_00, TIME_13_00);

        // then
        assertEquals(1, audioResults.size());
        AudioFileInfo firstSession = audioResults.get(0);

        assertEquals(TIME_12_00.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_12_02.toInstant(), firstSession.getEndTime());
    }

    @Test
    void processAudioForPlaybackWithOneAudioTrimmed() throws IOException, ExecutionException, InterruptedException {
        // given
        var mediaEntity1 = createMediaEntity(
            TIME_10_00,
            TIME_10_01,
            1,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath);

        // when
        List<AudioFileInfo> audioResults = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_10_00_15, TIME_10_00_45);

        // then
        AudioFileInfo firstSession = audioResults.get(0);

        assertEquals(TIME_10_00_15.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_10_00_45.toInstant(), firstSession.getEndTime());
    }

    @Test
    void processAudioForPlaybackShouldCallTrimWithExpectedArgumentsWhenDurationsIsPositive()
        throws ExecutionException, InterruptedException, IOException {
        //given
        var mediaEntity1 = createMediaEntity(
            TIME_12_09,
            TIME_12_10,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_12_10,
            TIME_12_11,
            2,
            1
        );
        var mediaEntityToDownloadLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath
        );

        // when
        List<AudioFileInfo> audioResults = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToDownloadLocation, TIME_12_09, TIME_13_00);

        // then
        AudioFileInfo firstSession = audioResults.get(0);

        assertEquals(TIME_12_09.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_12_11.toInstant(), firstSession.getEndTime());
    }

    @Test
    void processAudioForPlaybackShouldReturnTwoSessionsWhenProvidedAudiosWithVaryingStartEndTimesOnSameChannel()
        throws ExecutionException, InterruptedException, IOException {

        var mediaEntity1 = createMediaEntity(
            TIME_10_00,
            TIME_10_01,
            1,
            1
        );
        var mediaEntity2 = createMediaEntity(
            TIME_10_02,
            TIME_10_03,
            2,
            1
        );
        File audioFileTest = TestUtils.getFile(AUDIO_FILENAME);
        Path audioPath2 = Files.copy(audioFileTest.toPath(), createFile(tempDirectory, "audio-test2.mp2"), REPLACE_EXISTING);

        var mediaEntityToPlaybackLocation = Map.of(mediaEntity1, audioPath,
                                                   mediaEntity2, audioPath2
        );

        List<AudioFileInfo> sessions = outboundFileProcessor.processAudioForPlaybacks(mediaEntityToPlaybackLocation, TIME_10_00, TIME_10_03);

        assertEquals(2, sessions.size());
        AudioFileInfo firstSession = sessions.get(0);
        AudioFileInfo secondSession = sessions.get(1);

        assertEquals(TIME_10_00.toInstant(), firstSession.getStartTime());
        assertEquals(TIME_10_01.toInstant(), firstSession.getEndTime());
        assertEquals(TIME_10_02.toInstant(), secondSession.getStartTime());
        assertEquals(TIME_10_03.toInstant(), secondSession.getEndTime());

    }

    private MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int session, int channel) {
        var mediaEntity = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom",
                                                          startTime,
                                                          endTime,
                                                          channel
        );
        mediaEntity.setMediaFile(String.format("000%d.a0%d", session, channel - 1));
        mediaEntity.setMediaFormat("mpeg2");
        mediaEntity.setFileSize(961_024L);
        mediaEntity.setChecksum("3fab409db1e82c00df947a6e2a5cfa5d");
        mediaEntity.setMediaType('A');
        dartsDatabase.save(mediaEntity);
        return mediaEntity;
    }


    private Path createFile(Path path, String name) throws IOException {
        return Files.createFile(path.resolve(name));
    }

    @AfterEach
    void deleteFile() {
        if (tempDirectory != null) {
            try {
                FileUtils.forceDelete(tempDirectory.toFile());
            } catch (IOException e) {
                log.error("Unable to delete directory {}", tempDirectory);
            }
        }
    }
}
