package uk.gov.hmcts.darts.audio.controller;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.LogUtil;
import uk.gov.hmcts.darts.common.util.Version;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerAddAudioMetadataIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.of(2024, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC);
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private EventStub eventStub;
    @Autowired
    HearingStub hearingStub;
    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void addAudioMetadata() throws Exception {

        makeAddAudioCall();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(new Version().getVersionString(), media.getLegacyVersionLabel());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
    }

    private void makeAddAudioCall() throws Exception {
        makeAddAudioCall(1000L);
    }

    private void makeAddAudioCall(long fileSize) throws Exception {
        UserAccountEntity testUser = authorisationStub.getSystemUser();
        dartsDatabase.getUserAccountRepository().save(testUser);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        HearingEntity hearingForEvent = hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingDifferentCourtroom = hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingAfter = hearingStub.createHearing("Bristol", "1", "case3", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingAfter, 10, ENDED_AT.plusMinutes(20), "LOG");

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1", fileSize);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp3",
            "audio/mpeg",
            "Test Document (doc)".getBytes()
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );


        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void addAudioMetadataDuplicate() throws Exception {
        makeAddAudioCall();
        makeAddAudioCall();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(new Version().getVersionString(), media.getLegacyVersionLabel());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom

        Assertions.assertFalse(Objects.requireNonNull(LogUtil.getMemoryLogger()).searchLogs("Duplicate audio upload detected", ch.qos.logback.classic.Level.toLevel(
            Level.INFO_INT)).isEmpty());
    }

    @Test
    void addAudioMetadataVersionedDueToDuplicateFileSizeDifference() throws Exception {
        makeAddAudioCall();
        long differentFileSize = 1001L;
        makeAddAudioCall(differentFileSize);

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());

            Assertions.assertEquals(2, mediaEntities.size());

            MediaEntity media = mediaEntities.get(0);
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(new Version("1.0").getVersionString(), media.getLegacyVersionLabel());

            // lets make sure we have a second version of the file
            media = mediaEntities.get(1);
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(new Version("2.0").getVersionString(), media.getLegacyVersionLabel());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
        Assertions.assertFalse(Objects.requireNonNull(LogUtil.getMemoryLogger()).searchLogs("Uploading version of duplicate", ch.qos.logback.classic.Level.toLevel(
            Level.INFO_INT)).isEmpty());
    }

    @Test
    void addAudioMetadataNonExistingCourthouse() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "TEST", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp3",
            "audio/mpeg",
            "Test Document (doc)".getBytes()
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"COMMON_100","title":"Provided courthouse does not exist","status":400,"detail":"Courthouse 'TEST' not found."}""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt, String courthouse, String courtroom) {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, 1000L);
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt, String courthouse, String courtroom, long fileSize) {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format("mp3");
        addAudioMetadataRequest.filename("test");
        addAudioMetadataRequest.courthouse(courthouse);
        addAudioMetadataRequest.courtroom(courtroom);
        addAudioMetadataRequest.cases(List.of("case1", "case2", "case3"));
        addAudioMetadataRequest.setMediaFile("media file");
        addAudioMetadataRequest.setFileSize(fileSize);
        addAudioMetadataRequest.setChecksum("calculatedchecksum");
        return addAudioMetadataRequest;
    }
}