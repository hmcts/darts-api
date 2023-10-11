package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.testutils.data.DefendantTestData.createListOfDefendantsForCase;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Slf4j
class AudioRequestsControllerAddAudioRequestIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio-requests");
    private static final AudioRequestType AUDIO_REQUEST_TYPE = AudioRequestType.PLAYBACK;
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");
    private static final int REQUESTOR = 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("PMD.LawOfDemeter")
    @Test
    void addAudioRequestPostShouldReturnSuccess() throws Exception {
        assertEquals(0, dartsDatabase.getMediaRequestRepository()
            .findAll()
            .size(), "Precondition failed");

        HearingEntity hearingEntity = dartsDatabase.createHearing(
            "testCourthouse",
            "testCourtroom",
            "testCaseNumber",
            LocalDate.of(2023, 1, 1)
        );

        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setDefendantList(createListOfDefendantsForCase(2, courtCase));
        dartsDatabase.save(courtCase);
        dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        var audioRequestDetails = createAudioRequestDetails(hearingEntity);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
                "request_id": 1,
                "case_id": 1,
                "case_number": "testCaseNumber",
                "courthouse_name": "testCourthouse",
                "defendants": ["defendant_testCaseNumber_1","defendant_testCaseNumber_2"],
                "hearing_date": "2023-01-01",
                "start_time": "2023-01-01T12:00:00Z",
                "end_time": "2023-01-01T13:00:00Z"
            }
            """;
        log.info("actual json {}", actualJson);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

        List<MediaRequestEntity> mediaRequestEntities = dartsDatabase.getMediaRequestRepository()
            .findAll();
        assertEquals(1, mediaRequestEntities.size());

        var mediaRequestEntity = mediaRequestEntities.get(0);
        assertEquals(hearingEntity.getId(), mediaRequestEntity.getHearing().getId());
        assertEquals(REQUESTOR, mediaRequestEntity.getRequestor().getId());
        assertEquals(START_TIME, mediaRequestEntity.getStartTime());
        assertEquals(END_TIME, mediaRequestEntity.getEndTime());
        assertEquals(AUDIO_REQUEST_TYPE, mediaRequestEntity.getRequestType());
        assertEquals(AudioRequestStatus.OPEN, mediaRequestEntity.getStatus());
        assertEquals(0, mediaRequestEntity.getAttempts());

        List<NotificationEntity> notifications = dartsDatabase.getNotificationRepository().findByStatusIn(Collections.singletonList(NotificationStatus.OPEN));
        assertEquals(1, notifications.size());
        assertEquals(NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString(), notifications.get(0).getEventId());
        assertEquals(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity().getEmailAddress(), notifications.get(0).getEmailAddress());
        assertEquals(mediaRequestEntity.getHearing().getCourtCase().getCaseNumber(), notifications.get(0).getCourtCase().getCaseNumber());

        assertEquals(1, dartsDatabase.getAuditRepository().findAll().size());

    }

    private AudioRequestDetails createAudioRequestDetails(HearingEntity hearingEntity) {
        var audioRequestDetails = new AudioRequestDetails();

        audioRequestDetails.setRequestType(AUDIO_REQUEST_TYPE);
        audioRequestDetails.setHearingId(hearingEntity.getId());
        audioRequestDetails.setStartTime(START_TIME);
        audioRequestDetails.setEndTime(END_TIME);
        audioRequestDetails.setRequestor(REQUESTOR);

        return audioRequestDetails;
    }

}
