package uk.gov.hmcts.darts.audio.controller;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createDefenceForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCaseWithName;

@AutoConfigureMockMvc
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioRequestsControllerAddAudioRequestIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio-requests");
    private static final String HEARING_DATETIME = "2023-01-01T10:00:00";
    private static final String HEARING_DATE = "2023-01-01";
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";
    private static final String SOME_START_TIME = "2023-01-01T12:00:00Z";
    private static final String SOME_END_TIME = "2023-01-01T13:00:00Z";

    private static final AudioRequestType AUDIO_REQUEST_TYPE_PLAYBACK = AudioRequestType.PLAYBACK;
    private static final AudioRequestType AUDIO_REQUEST_TYPE_DOWNLOAD = AudioRequestType.DOWNLOAD;
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse(SOME_START_TIME);
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse(SOME_END_TIME);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private HearingEntity hearingEntity;
    private UserAccountEntity testUser;

    @BeforeEach
    void beforeEach() {
        hearingEntity = PersistableFactory.getHearingTestData().hearingWith(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            HEARING_DATETIME
        );
        var courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor(createProsecutorForCaseWithName(courtCase, "aProsecutor"));
        courtCase.addDefendant(createDefendantForCaseWithName(courtCase, "aDefendant"));
        courtCase.addDefence(createDefenceForCaseWithName(courtCase, "aDefence"));
        dartsPersistence.save(hearingEntity);

        testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void addAudioRequestPostShouldReturnForbiddenError() throws Exception {

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        var audioRequestDetails = createAudioRequestDetails(hearingEntity, AUDIO_REQUEST_TYPE_DOWNLOAD);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }

    @Test
    void addAudioRequestPostShouldReturnSuccess() throws Exception {

        var audioRequestDetails = createAudioRequestDetails(hearingEntity, AUDIO_REQUEST_TYPE_PLAYBACK);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNumber())
            .andExpect(jsonPath("$.case_id").isNumber())
            .andExpect(jsonPath("$.case_number").value(SOME_CASE_NUMBER))
            .andExpect(jsonPath("$.courthouse_name").value(SOME_COURTHOUSE))
            .andExpect(jsonPath("$.defendants").isNotEmpty())
            .andExpect(jsonPath("$.hearing_date").value(HEARING_DATE))
            .andExpect(jsonPath("$.start_time").value(SOME_START_TIME))
            .andExpect(jsonPath("$.end_time").value(SOME_END_TIME))
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        Integer mediaRequestId = JsonPath.parse(actualJson).read("$.request_id");
        assertNotNull(mediaRequestId);

        MediaRequestEntity mediaRequestEntity = dartsDatabase.getMediaRequestRepository().findById(mediaRequestId)
            .orElseThrow();

        assertEquals(hearingEntity.getId(), mediaRequestEntity.getHearing().getId());
        assertEquals(testUser.getId(), mediaRequestEntity.getRequestor().getId());
        assertEquals(START_TIME, mediaRequestEntity.getStartTime());
        assertEquals(END_TIME, mediaRequestEntity.getEndTime());
        assertEquals(AUDIO_REQUEST_TYPE_PLAYBACK, mediaRequestEntity.getRequestType());
        assertEquals(MediaRequestStatus.OPEN, mediaRequestEntity.getStatus());
        assertEquals(0, mediaRequestEntity.getAttempts());

        List<NotificationEntity> notifications = dartsDatabase.getNotificationRepository()
            .findByStatusIn(Collections.singletonList(NotificationStatus.OPEN));
        assertEquals(1, notifications.size());
        assertEquals(
            NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString(),
            notifications.getFirst().getEventId()
        );
        assertEquals(
            dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity().getEmailAddress(),
            notifications.getFirst().getEmailAddress()
        );
        assertEquals(
            mediaRequestEntity.getHearing().getCourtCase().getCaseNumber(),
            notifications.getFirst().getCourtCase().getCaseNumber()
        );

        assertEquals(1, dartsDatabase.getAuditRepository().findAll().size());

    }

    @Test
    void addAudioRequestDownloadPostShouldReturnSuccessForTranscriber() throws Exception {
        var audioRequestDetails = createAudioRequestDetails(hearingEntity, AUDIO_REQUEST_TYPE_DOWNLOAD);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNumber())
            .andExpect(jsonPath("$.case_id").isNumber())
            .andExpect(jsonPath("$.case_number").value(SOME_CASE_NUMBER))
            .andExpect(jsonPath("$.courthouse_name").value(SOME_COURTHOUSE))
            .andExpect(jsonPath("$.defendants").isNotEmpty())
            .andExpect(jsonPath("$.hearing_date").value(HEARING_DATE))
            .andExpect(jsonPath("$.start_time").value(SOME_START_TIME))
            .andExpect(jsonPath("$.end_time").value(SOME_END_TIME))
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        Integer mediaRequestId = JsonPath.parse(actualJson).read("$.request_id");
        assertNotNull(mediaRequestId);

        MediaRequestEntity mediaRequestEntity = dartsDatabase.getMediaRequestRepository().findById(mediaRequestId)
            .orElseThrow();

        assertEquals(hearingEntity.getId(), mediaRequestEntity.getHearing().getId());
        assertEquals(testUser.getId(), mediaRequestEntity.getRequestor().getId());
        assertEquals(START_TIME, mediaRequestEntity.getStartTime());
        assertEquals(END_TIME, mediaRequestEntity.getEndTime());
        assertEquals(AUDIO_REQUEST_TYPE_DOWNLOAD, mediaRequestEntity.getRequestType());
        assertEquals(MediaRequestStatus.OPEN, mediaRequestEntity.getStatus());
        assertEquals(0, mediaRequestEntity.getAttempts());

        List<NotificationEntity> notifications = dartsDatabase.getNotificationRepository()
            .findByStatusIn(Collections.singletonList(NotificationStatus.OPEN));
        assertEquals(1, notifications.size());
        assertEquals(
            NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString(),
            notifications.getFirst().getEventId()
        );
        assertEquals(
            dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity().getEmailAddress(),
            notifications.getFirst().getEmailAddress()
        );
        assertEquals(
            mediaRequestEntity.getHearing().getCourtCase().getCaseNumber(),
            notifications.getFirst().getCourtCase().getCaseNumber()
        );

        assertEquals(1, dartsDatabase.getAuditRepository().findAll().size());

    }

    @Test
    void addAudioRequestDownloadPostShouldThrow403() throws Exception {
        CourthouseEntity courthouse = dartsDatabase.findCourthouseWithName(SOME_COURTHOUSE);
        testUser = dartsDatabase.getUserAccountStub().createJudgeUser(courthouse);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        var audioRequestDetails = createAudioRequestDetails(hearingEntity, AUDIO_REQUEST_TYPE_DOWNLOAD);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void addAudioRequestWhenHearingNotFoundShouldThrow404() throws Exception {
        var audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setHearingId(9999); // Set to not existing hearing id
        audioRequestDetails.setStartTime(START_TIME);
        audioRequestDetails.setEndTime(END_TIME);
        audioRequestDetails.setRequestor(testUser.getId());
        audioRequestDetails.setRequestType(AUDIO_REQUEST_TYPE_DOWNLOAD);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    void duplicateAddAudioRequestShouldThrowConflictError() throws Exception {
        var requestor = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser("NEWCASTLE");

        var audioRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        when(mockUserIdentity.getUserAccount()).thenReturn(requestor);

        var audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setRequestType(audioRequestEntity.getRequestType());
        audioRequestDetails.setHearingId(audioRequestEntity.getHearing().getId());
        audioRequestDetails.setStartTime(audioRequestEntity.getStartTime());
        audioRequestDetails.setEndTime(audioRequestEntity.getEndTime());
        audioRequestDetails.setRequestor(audioRequestEntity.getRequestor().getId());

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("AUDIO_REQUESTS_104"))
            .andReturn();
    }

    private AudioRequestDetails createAudioRequestDetails(HearingEntity hearingEntity, AudioRequestType audioRequestTypeDownload) {
        var audioRequestDetails = new AudioRequestDetails();

        audioRequestDetails.setRequestType(audioRequestTypeDownload);
        audioRequestDetails.setHearingId(hearingEntity.getId());
        audioRequestDetails.setStartTime(START_TIME);
        audioRequestDetails.setEndTime(END_TIME);
        audioRequestDetails.setRequestor(testUser.getId());

        return audioRequestDetails;
    }

}