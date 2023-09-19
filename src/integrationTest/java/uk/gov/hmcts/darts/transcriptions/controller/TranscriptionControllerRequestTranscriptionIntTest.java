package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createJudgeWithName;


@AutoConfigureMockMvc
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerRequestTranscriptionIntTest extends IntegrationBase {

    private static final String ENDPOINT = "/transcriptions";

    private static final URI ENDPOINT_URI = URI.create(ENDPOINT);

    private static final String TEST_COMMENT = "Test comment";

    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-07-31T12:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-07-31T14:32Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private CourtCaseEntity courtCase;
    private HearingEntity hearing;

    @BeforeEach
    void setupData() {
        CourthouseEntity someCourthouse = someMinimalCourthouse();
        someCourthouse.setCourthouseName(SOME_COURTHOUSE);

        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(someCourthouse, SOME_COURTROOM);

        courtCase = createCaseAt(someCourthouse);
        courtCase.setCaseNumber("Case1");

        JudgeEntity judge = createJudgeWithName("aJudge");
        hearing = createHearingWithDefaults(courtCase, courtroom1, LocalDate.of(2023, 5, 20), judge);

        dartsDatabase.saveAll(hearing);
    }

    @Test
    @Order(1)
    @Transactional
    void transcriptionRequestWithValidValuesShouldReturnSuccess() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

    }

    @Test
    @Order(2)
    @Transactional
    void transcriptionRequestWithNullDatesAndSentencingRemarksTypeShouldReturnSuccess()
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.SENTENCING_REMARKS;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, null, null
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

    }

    @Test
    @Order(3)
    @Transactional
    void transcriptionRequestWithNullHearingAndNullCaseShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, null, transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
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
    void transcriptionRequestWithNullStartDateAndRequiredDatesTranscriptionTypeShouldThrowException(TranscriptionTypeEnum transcriptionTypeEnum)
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, null, transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, null, END_TIME
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
    void transcriptionRequestWithNullEndDateAndRequiredDatesTranscriptionTypeShouldThrowException(TranscriptionTypeEnum transcriptionTypeEnum)
        throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            null, null, transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, null
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
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));


        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andReturn();

    }

    @Test
    @Order(7)
    @Transactional
    void transcriptionRequestWithNullUrgencyIdShouldThrowException() throws Exception {
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            hearing.getId(), courtCase.getId(), null,
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
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
            hearing.getId(), courtCase.getId(), transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            123, TEST_COMMENT, START_TIME, END_TIME
        );

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URI)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(transcriptionRequestDetails));


        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andReturn();

    }

    @Test
    @Order(9)
    @Transactional
    void transcriptionRequestWithInvalidHearingIdShouldThrowException() throws Exception {
        TranscriptionUrgencyEnum transcriptionUrgencyEnum = TranscriptionUrgencyEnum.STANDARD;
        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.COURT_LOG;

        TranscriptionRequestDetails transcriptionRequestDetails = createTranscriptionRequestDetails(
            789, null, transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
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
            null, 789, transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
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
            hearing.getId(), null, transcriptionUrgencyEnum.getTranscriptionUrgencyKey(),
            transcriptionTypeEnum.getTranscriptionTypeKey(), TEST_COMMENT, START_TIME, END_TIME
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
