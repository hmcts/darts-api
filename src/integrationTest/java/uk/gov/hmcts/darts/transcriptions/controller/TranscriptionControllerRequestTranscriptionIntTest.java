package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;


@AutoConfigureMockMvc
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerRequestTranscriptionIntTest extends IntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions");

    private static final String TEST_COMMENT = "Test comment";

    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-07-31T12:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-07-31T14:32Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private ObjectMapper objectMapper;

    private CourtCaseEntity courtCase;
    private HearingEntity hearing;
    private UserAccountEntity testUser;

    @BeforeEach
    void setupData() {
        authorisationStub.givenTestSchema();

        courtCase = authorisationStub.getCourtCaseEntity();
        hearing = authorisationStub.getHearingEntity();
        testUser = authorisationStub.getTestUser();

        when(mockUserIdentity.getEmailAddress()).thenReturn(testUser.getEmailAddress());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    @Order(1)
    @Transactional
    void transcriptionRequestWithValidValuesShouldReturnSuccess() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

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

        TranscriptionRepository transcriptionRepository = dartsDatabaseStub.getTranscriptionRepository();
        TranscriptionEntity transcriptionEntity = transcriptionRepository.findById(transcriptionId).orElseThrow();
        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionEntity.getTranscriptionWorkflowEntities();
        assertEquals(2, transcriptionWorkflowEntities.size());
        assertTranscriptionWorkflow(transcriptionWorkflowEntities.get(0),
                                    REQUESTED, testUser, TEST_COMMENT
        );
        assertTranscriptionWorkflow(transcriptionWorkflowEntities.get(1),
                                    AWAITING_AUTHORISATION, testUser, null
        );
    }

    private void assertTranscriptionWorkflow(TranscriptionWorkflowEntity transcriptionWorkflowToCheck,
                                             TranscriptionStatusEnum expectedTranscriptionStatus,
                                             UserAccountEntity expectedWorkflowActor,
                                             String expectedWorkflowComment) {

        assertEquals(
            expectedTranscriptionStatus.getId(),
            transcriptionWorkflowToCheck.getTranscriptionStatus().getId()
        );
        assertEquals(expectedWorkflowActor, transcriptionWorkflowToCheck.getWorkflowActor());
        assertEquals(expectedWorkflowComment, transcriptionWorkflowToCheck.getWorkflowComment());
    }

    @Test
    @Order(2)
    @Transactional
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
    }

    @Test
    @Order(3)
    @Transactional
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

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        assertTranscriptionFailed100Error(actualJson);
    }

    @ParameterizedTest
    @EnumSource(names = {"COURT_LOG", "SPECIFIED_TIMES"})
    @Order(4)
    @Transactional
    void transcriptionRequestWithNullStartDateAndRequiredDatesTranscriptionTypeShouldThrowException(
        TranscriptionTypeEnum transcriptionTypeEnum)
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, null, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        assertTranscriptionFailed100Error(actualJson);
    }

    @ParameterizedTest
    @EnumSource(names = {"COURT_LOG", "SPECIFIED_TIMES"})
    @Order(5)
    @Transactional
    void transcriptionRequestWithNullEndDateAndRequiredDatesTranscriptionTypeShouldThrowException(
        TranscriptionTypeEnum transcriptionTypeEnum)
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, null
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        assertTranscriptionFailed100Error(actualJson);
    }

    @Test
    @Order(6)
    @Transactional
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
    }

    @Test
    @Order(7)
    @Transactional
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

    }

    @Test
    @Order(8)
    @Transactional
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
    }

    @Test
    @Order(9)
    @Transactional
    void transcriptionRequestWithInvalidHearingIdShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            789, null, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "type": "HEARING_100",
              "title": "The requested hearing cannot be found",
              "status": 404
            }
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @Order(10)
    @Transactional
    void transcriptionRequestWithInvalidCaseIdShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, 789, transcriptionUrgencyEnum.getId(),
            transcriptionTypeEnum.getId(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "type": "CASE_104",
              "title": "The requested case cannot be found",
              "status": 404
            }""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @Order(11)
    @Transactional
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
    }

    private static void assertTranscriptionFailed100Error(String actualJson) {
        String expectedJson = """
            {
              "type": "TRANSCRIPTION_100",
              "title": "Failed to validate transcription request",
              "status": 400
            }""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
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
        transcriptionRequestDetails.setUrgencyId(urgencyId);
        transcriptionRequestDetails.setTranscriptionTypeId(transcriptionTypeId);
        transcriptionRequestDetails.setComment(comment);
        transcriptionRequestDetails.setStartDateTime(startDateTime);
        transcriptionRequestDetails.setEndDateTime(endDateTime);
        return transcriptionRequestDetails;
    }
}
