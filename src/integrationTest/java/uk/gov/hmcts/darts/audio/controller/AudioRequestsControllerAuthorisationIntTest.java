package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@AutoConfigureMockMvc
class AudioRequestsControllerAuthorisationIntTest extends IntegrationBase {

    private static final OffsetDateTime MEDIA_START = OffsetDateTime.parse("2023-01-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;
    @Autowired
    private TransientObjectDirectoryStub transientObjectDirectoryStub;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "AUDIO-REQUEST-AUTH-CASE", "AUDIO-REQUEST-AUTH-COURTHOUSE", "audio-request-auth-courtroom", LocalDateTime.now()
        );
        var media = PersistableFactory.getMediaTestData()
            .createMediaWith(hearing.getCourtroom(), MEDIA_START, MEDIA_START.plusHours(1), 1);
        hearing.addMedia(media);
        dartsPersistence.save(hearing);
        dartsPersistence.save(PersistableFactory.getExternalObjectDirectoryTestData()
                                  .eodStoredInUnstructuredLocationForMedia(media));

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = INCLUDE)
    void audioRequestEndpoints_shouldAllowAccess_whenRoleHasConfiguredGlobalAccess(SecurityRoleEnum role) throws Exception {
        UserAccountEntity user = given.anAuthenticatedUserWithGlobalAccessAndRole(role);
        MediaRequestEntity openPlaybackRequest = createPlaybackRequest(user, MediaRequestStatus.OPEN);
        Integer playbackTransformedMediaId = createStoredTransformedMediaFor(createPlaybackRequest(user, MediaRequestStatus.COMPLETED));

        mockMvc.perform(delete("/audio-requests/{media_request_id}", openPlaybackRequest.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/audio-requests/playback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createAudioRequestDetails(user))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/audio-requests/playback")
                            .queryParam("transformed_media_id", String.valueOf(playbackTransformedMediaId)))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/audio-requests/transformed_media/{transformed_media_id}", playbackTransformedMediaId))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/audio-requests/transformed_media/{transformed_media_id}", playbackTransformedMediaId))
            .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = EXCLUDE)
    void audioRequestEndpoints_shouldForbidAccess_whenRoleLacksConfiguredGlobalAccess(SecurityRoleEnum role) throws Exception {
        UserAccountEntity user = given.anAuthenticatedUserWithGlobalAccessAndRole(role);
        MediaRequestEntity openPlaybackRequest = createPlaybackRequest(user, MediaRequestStatus.OPEN);
        Integer playbackTransformedMediaId = createStoredTransformedMediaFor(createPlaybackRequest(user, MediaRequestStatus.COMPLETED));

        mockMvc.perform(delete("/audio-requests/{media_request_id}", openPlaybackRequest.getId()))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/audio-requests/playback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createAudioRequestDetails(user))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/audio-requests/playback")
                            .queryParam("transformed_media_id", String.valueOf(playbackTransformedMediaId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/audio-requests/transformed_media/{transformed_media_id}", playbackTransformedMediaId))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/audio-requests/transformed_media/{transformed_media_id}", playbackTransformedMediaId))
            .andExpect(status().isForbidden());
    }

    private MediaRequestEntity createPlaybackRequest(UserAccountEntity requestor, MediaRequestStatus status) {
        return dartsDatabase.getMediaRequestStub()
            .createAndLoadMediaRequestEntity(requestor, requestor, hearing, AudioRequestType.PLAYBACK, status,
                                             MEDIA_START.plusMinutes(40), MEDIA_START.plusMinutes(50), OffsetDateTime.now());
    }

    private Integer createStoredTransformedMediaFor(MediaRequestEntity mediaRequest) {
        return transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
            mediaRequest,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            UUID.randomUUID().toString()
        ).getTransformedMedia().getId();
    }

    private AudioRequestDetails createAudioRequestDetails(UserAccountEntity requestor) {
        var audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setHearingId(hearing.getId());
        audioRequestDetails.setStartTime(MEDIA_START.plusMinutes(10));
        audioRequestDetails.setEndTime(MEDIA_START.plusMinutes(20));
        audioRequestDetails.setRequestor(requestor.getId());
        return audioRequestDetails;
    }
}
