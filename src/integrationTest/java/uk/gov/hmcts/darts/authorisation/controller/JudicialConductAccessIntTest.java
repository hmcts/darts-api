package uk.gov.hmcts.darts.authorisation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.service.AudioPreviewService;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIAL_CONDUCT;

@AutoConfigureMockMvc
class JudicialConductAccessIntTest extends IntegrationBase {

    private static final OffsetDateTime HEARING_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnotationStub annotationStub;

    @Autowired
    private TransientObjectDirectoryStub transientObjectDirectoryStub;

    @MockitoBean
    private AudioPreviewService audioPreviewService;

    private HearingEntity hearing;
    private UserAccountEntity judicialConductUser;
    private MediaRequestEntity openPlaybackRequest;
    private Integer playbackTransformedMediaId;
    private Long mediaId;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "JCO-CASE-1",
            "JCO-COURTHOUSE",
            "jco-courtroom",
            DateConverterUtil.toLocalDateTime(HEARING_DATE_TIME)
        );
        dartsDatabase.createEvent(hearing);
        addMediaToHearing();

        UserAccountEntity requestor = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearing.getCourtroom().getCourthouse());
        dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            requestor,
            hearing.getCourtCase(),
            hearing,
            HEARING_DATE_TIME,
            false
        );
        annotationStub.createAndSaveAnnotationEntityWith(requestor, "JCO should not see this annotation", hearing);

        judicialConductUser = givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIAL_CONDUCT);
        openPlaybackRequest = createPlaybackRequest(MediaRequestStatus.OPEN);
        playbackTransformedMediaId = createStoredTransformedMediaFor(createPlaybackRequest(MediaRequestStatus.COMPLETED));
    }

    @Test
    void judicialConductUserRequest_shouldAccessCaseFileTabsAcrossAllCourts_whenGlobalAccessIsAuthorised() throws Exception {
        Integer caseId = hearing.getCourtCase().getId();

        mockMvc.perform(get("/cases/{case_id}", caseId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/cases/{case_id}/hearings", caseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)))
            .andExpect(jsonPath("$[0].id", is(hearing.getId())));

        mockMvc.perform(get("/cases/{case_id}/events", caseId)
                            .queryParam("page_number", "1")
                            .queryParam("page_size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)));

        mockMvc.perform(get("/cases/{case_id}/transcripts", caseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void judicialConductUserRequest_shouldAccessHearingDetailsEventsAndAudioAcrossAllCourts_whenGlobalAccessIsAuthorised() throws Exception {
        Integer hearingId = hearing.getId();

        mockMvc.perform(get("/hearings/{hearingId}", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hearing_id", is(hearingId)));

        mockMvc.perform(get("/hearings/{hearingId}/events", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));

        mockMvc.perform(get("/audio/hearings/{hearing_id}/audios", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void judicialConductUserRequest_shouldNotAccessAnnotations_whenAccessIsNotAuthorised() throws Exception {
        mockMvc.perform(get("/cases/{case_id}/annotations", hearing.getCourtCase().getId()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/hearings/{hearingId}/annotations", hearing.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void judicialConductUserRequest_shouldAccessPlaybackAudioAcrossAllCourts_whenGlobalAccessIsAuthorised() throws Exception {
        when(audioPreviewService.getOrCreateAudioPreview(mediaId))
            .thenReturn(new AudioPreview(mediaId, READY, "preview audio".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/audio-requests/playback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createAudioRequestDetails())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNumber())
            .andExpect(jsonPath("$.case_id", is(hearing.getCourtCase().getId())))
            .andExpect(jsonPath("$.case_number", is(hearing.getCourtCase().getCaseNumber())))
            .andExpect(jsonPath("$.courthouse_name", is(hearing.getCourtroom().getCourthouse().getCourthouseName())));

        mockMvc.perform(get("/audio/preview/{media_id}", mediaId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/audio-requests/playback")
                            .queryParam("transformed_media_id", String.valueOf(playbackTransformedMediaId)))
            .andExpect(status().isOk());
    }

    @Test
    void judicialConductUserRequest_shouldManagePlaybackAudioAcrossAllCourts_whenGlobalAccessIsAuthorised() throws Exception {
        mockMvc.perform(patch("/audio-requests/transformed_media/{transformed_media_id}", playbackTransformedMediaId))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/audio-requests/{media_request_id}", openPlaybackRequest.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/audio-requests/transformed_media/{transformed_media_id}", playbackTransformedMediaId))
            .andExpect(status().isNoContent());
    }

    private void addMediaToHearing() {
        var media = PersistableFactory.getMediaTestData()
            .createMediaWith(hearing.getCourtroom(), MEDIA_START_TIME, MEDIA_END_TIME, 1);
        hearing.addMedia(media);
        dartsPersistence.save(hearing);
        dartsPersistence.save(PersistableFactory.getExternalObjectDirectoryTestData()
                                  .eodStoredInUnstructuredLocationForMedia(media));
        mediaId = media.getId();
    }

    private MediaRequestEntity createPlaybackRequest(MediaRequestStatus status) {
        return dartsDatabase.getMediaRequestStub()
            .createAndLoadMediaRequestEntity(judicialConductUser, judicialConductUser, hearing, AudioRequestType.PLAYBACK, status,
                                             MEDIA_START_TIME.plusMinutes(40), MEDIA_START_TIME.plusMinutes(50), OffsetDateTime.now());
    }

    private Integer createStoredTransformedMediaFor(MediaRequestEntity mediaRequest) {
        return transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
            mediaRequest,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            UUID.randomUUID().toString()
        ).getTransformedMedia().getId();
    }

    private AudioRequestDetails createAudioRequestDetails() {
        var audioRequestDetails = new AudioRequestDetails();

        audioRequestDetails.setHearingId(hearing.getId());
        audioRequestDetails.setStartTime(MEDIA_START_TIME.plusMinutes(10));
        audioRequestDetails.setEndTime(MEDIA_START_TIME.plusMinutes(20));
        audioRequestDetails.setRequestor(judicialConductUser.getId());

        return audioRequestDetails;
    }
}
