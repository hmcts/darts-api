package uk.gov.hmcts.darts.transcriptions.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REQUEST_TRANSCRIPTION;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.COURT_MANAGER_APPROVE_TRANSCRIPT;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

@AutoConfigureMockMvc
@SuppressWarnings({"PMD.ExcessiveImports"})
@Transactional
class TranscriptionControllerRequestTranscriptionIntTest extends IntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions");

    private static final String TEST_COMMENT = "Test comment";

    private static final OffsetDateTime START_TIME = now().plusMinutes(5);
    private static final OffsetDateTime END_TIME = now().plusMinutes(20);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    private CourtCaseEntity courtCase;
    private HearingEntity hearing;
    private UserAccountEntity testUser;

    @BeforeEach
    void setupData() {
        authorisationStub.givenTestSchema();

        courtCase = authorisationStub.getCourtCaseEntity();
        hearing = authorisationStub.getHearingEntity();
        testUser = authorisationStub.getTestUser();

        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @ParameterizedTest
    @EnumSource(names = {"COURT_LOG", "SPECIFIED_TIMES","OTHER"})
    void transcriptionRequestWithValidValuesShouldReturnSuccess(TranscriptionTypeEnum transcriptionTypeEnum) throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionId = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.transcription_id");
        assertNotNull(transcriptionId);

        TranscriptionRepository transcriptionRepository = dartsDatabase.getTranscriptionRepository();
        TranscriptionEntity transcriptionEntity = transcriptionRepository.findById(transcriptionId).orElseThrow();
        assertTrue(transcriptionEntity.getIsManualTranscription());
        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionEntity.getTranscriptionWorkflowEntities();
        assertEquals(2, transcriptionWorkflowEntities.size());
        assertTranscriptionWorkflow(transcriptionWorkflowEntities.get(0),
                                    REQUESTED, testUser
        );
        assertTranscriptionWorkflow(transcriptionWorkflowEntities.get(1),
                                    AWAITING_AUTHORISATION, testUser
        );

        assertThat(dartsDatabase.getTranscriptionCommentRepository().findAll())
            .hasSize(1)
            .extracting(TranscriptionCommentEntity::getComment)
            .containsExactly(TEST_COMMENT);

        List<NotificationEntity> notificationEntities = dartsDatabase.getNotificationRepository().findAll();
        List<String> templateList = notificationEntities.stream().map(NotificationEntity::getEventId).toList();
        assertTrue(templateList.contains(COURT_MANAGER_APPROVE_TRANSCRIPT.toString()));

        assertAudit(1);
    }

    private void assertTranscriptionWorkflow(TranscriptionWorkflowEntity transcriptionWorkflowToCheck,
                                             TranscriptionStatusEnum expectedTranscriptionStatus,
                                             UserAccountEntity expectedWorkflowActor) {

        assertEquals(
            expectedTranscriptionStatus.getId(),
            transcriptionWorkflowToCheck.getTranscriptionStatus().getId()
        );
        assertEquals(expectedWorkflowActor, transcriptionWorkflowToCheck.getWorkflowActor());
    }

    private void assertAudit(int expected) {

        Integer courtCaseId = courtCase.getId();
        OffsetDateTime fromDate = now().minusDays(1);
        OffsetDateTime toDate = now().plusDays(1);
        List<AuditEntity> auditEntities = auditRepository.getAuditEntitiesByCaseAndActivityForDateRange(courtCaseId,
                                                                                                        REQUEST_TRANSCRIPTION.getId(),
                                                                                                        fromDate, toDate);

        assertEquals(expected, auditEntities.size());
        if (expected == 1) {
            assertEquals(testUser, auditEntities.get(0).getUser());
        }
    }

    @Test
    void transcriptionRequestWithDuplicateValues() throws Exception {
        OffsetDateTime startTime = now().plusMinutes(5).truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime endTime = now().plusMinutes(10).truncatedTo(ChronoUnit.SECONDS);

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SPECIFIED_TIMES;

        var dupeTranscription = transcriptionStub.createAndSaveCompletedTranscription(
            testUser, courtCase, hearing,startTime, endTime, now(), false);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, startTime, endTime
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_107")))
            .andExpect(jsonPath("$.properties.duplicate_transcription_id", is(dupeTranscription.getId())));
    }

    @Test
    void transcriptionRequestWithDuplicateValuesWithNoTimes() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        TranscriptionTypeEntity transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(TranscriptionTypeEnum.SENTENCING_REMARKS.getId());

        TranscriptionEntity dupeTranscription = transcriptionStub.createAndSaveCompletedTranscription(
            testUser, courtCase, hearing, null, null, transcriptionType, now(), false);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, null, null
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_107")))
            .andExpect(jsonPath("$.properties.duplicate_transcription_id", is(dupeTranscription.getId())));
    }


    @Test
    void transcriptionRequestHearingWithNoAudio() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        hearing.setMediaList(null);
        dartsDatabase.save(hearing);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_110")))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.title", is("Transcription could not be requested, no audio")));

        assertAudit(0);
    }


    @Test
    void transcriptionRequestStartTimeOutsideHearing() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, now().minusHours(1), now().plusMinutes(10)
        );

        dartsDatabase.save(hearing);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_111")))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.title", is("Transcription could not be requested, times outside of hearing times")));

        assertAudit(0);
    }

    @Test
    void transcriptionRequestEndTimeOutsideHearing() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, now().plusMinutes(1), now().plusHours(10)
        );

        dartsDatabase.save(hearing);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

       mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_111")))
            .andExpect(jsonPath("$.status", is(404)))
           .andExpect(jsonPath("$.title", is("Transcription could not be requested, times outside of hearing times")));

        assertAudit(0);
    }

    @Test
    void transcriptionRequestExactStartAndEnd() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        OffsetDateTime startTime = hearing.getMediaList().get(0).getStart().truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime endTime = hearing.getMediaList().get(0).getEnd().truncatedTo(ChronoUnit.SECONDS);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, startTime, endTime
        );

        dartsDatabase.save(hearing);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionId = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.transcription_id");
        assertNotNull(transcriptionId);

        assertAudit(1);
    }

    @Test
    void transcriptionRequestInsideStartAndEnd() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        OffsetDateTime startTime = hearing.getMediaList().get(0).getStart().plusMinutes(5);
        OffsetDateTime endTime = hearing.getMediaList().get(0).getEnd().minusMinutes(15);

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, startTime, endTime
        );

        dartsDatabase.save(hearing);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionId = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.transcription_id");
        assertNotNull(transcriptionId);

        assertAudit(1);
    }

    @Test
    void transcriptionRequestWithNullDatesAndSentencingRemarksTypeShouldReturnSuccess()
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, null, null
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionId = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.transcription_id");
        assertNotNull(transcriptionId);

        assertAudit(1);
    }

    @Test
    void transcriptionRequestWithNullHearingAndNullCaseShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type", is("AUTHORISATION_107")))
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.title", is("Failed to check authorisation")));

        assertAudit(0);
    }

    @ParameterizedTest
    @EnumSource(names = {"COURT_LOG", "SPECIFIED_TIMES"})
    void transcriptionRequestWithNullStartDateAndRequiredDatesTranscriptionTypeShouldThrowException(
        TranscriptionTypeEnum transcriptionTypeEnum)
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, null, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_100")))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.title", is("Failed to validate transcription request")));

        assertAudit(0);
    }

    @ParameterizedTest
    @EnumSource(names = {"COURT_LOG", "SPECIFIED_TIMES"})
    void transcriptionRequestWithNullEndDateAndRequiredDatesTranscriptionTypeShouldThrowException(
        TranscriptionTypeEnum transcriptionTypeEnum)
        throws Exception {

        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, null
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_100")))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.title", is("Failed to validate transcription request")));

        assertAudit(0);
    }

    @Test
    void transcriptionRequestWithInvalidUrgencyIdShouldThrowException() throws Exception {
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), 123,
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_106")));

        assertAudit(0);
    }

    @Test
    void transcriptionRequestWithNullUrgencyIdShouldThrowException() throws Exception {
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), null,
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertAudit(0);
    }

    @Test
    void transcriptionRequestWithInvalidTranscriptionTypeIdShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.OVERNIGHT;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getId(),
            123, TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type", is("TRANSCRIPTION_104")));

        assertAudit(0);
    }

    @Test
    void transcriptionRequestWithInvalidHearingIdShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            999_999, null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type", is("HEARING_100")))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.title", is("The requested hearing cannot be found")));


        assertAudit(0);
    }

    @Test
    void transcriptionRequestWithInvalidCaseIdShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, 999_999, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

         mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type", is("CASE_104")))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.title", is("The requested case cannot be found")));

        assertAudit(0);
    }

    @Test
    void transcriptionRequestWithValidHearingAndNullCaseIdShouldReturnSuccess() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        assertAudit(1);
    }

    private TranscriptionRequestDetails createTranscriptionRequestDetails(Integer hearingId,
                                                                          Integer caseId,
                                                                          Integer urgencyId,
                                                                          Integer transcriptionTypeId,
                                                                          String comment,
                                                                          OffsetDateTime startDateTime,
                                                                          OffsetDateTime endDateTime
    ) {
        TranscriptionRequestDetails transcriptionRequestDetails = new TranscriptionRequestDetails();
        transcriptionRequestDetails.setHearingId(hearingId);
        transcriptionRequestDetails.setCaseId(caseId);
        transcriptionRequestDetails.setTranscriptionUrgencyId(urgencyId);
        transcriptionRequestDetails.setTranscriptionTypeId(transcriptionTypeId);
        transcriptionRequestDetails.setComment(comment);
        transcriptionRequestDetails.setStartDateTime(startDateTime);
        transcriptionRequestDetails.setEndDateTime(endDateTime);
        return transcriptionRequestDetails;
    }


}