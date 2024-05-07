package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class TranscriptionControllerGetTranscriptionWorkflowsIntTest extends IntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/admin/transcription-workflows");
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserIdentity mockUserIdentity;
    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    private TranscriptionEntity transcription;

    @BeforeEach
    void beforeEach() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourthouseEntity courthouseEntity = hearingEntity.getCourtroom().getCourthouse();
        assertEquals(SOME_COURTHOUSE, courthouseEntity.getCourthouseName());

        transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2024, 4, 24, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);

        createWorkflowEntity(transcription,
                             OffsetDateTime.of(2024, 4, 23, 10, 0, 0, 0, ZoneOffset.UTC),
                             dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(TranscriptionStatusEnum.REQUESTED),
                             "comment1");

        createWorkflowEntity(transcription,
                             OffsetDateTime.of(2024, 4, 24, 12, 0, 0, 0, ZoneOffset.UTC),
                             dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(TranscriptionStatusEnum.APPROVED),
                             "comment2");
    }

    @Test
    void getTranscriptionWorkflowsShouldReturnOkSuccessAndAllAssociatedWorkflows() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .queryParam("transcription_id", transcription.getId().toString());

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/transcriptions/transcription_workflow/expectedAllWorkflowResponse.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getCurrentTranscriptionWorkflowsShouldReturnOkSuccessAndMostRecentAssociatedWorkflows() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .queryParam("transcription_id", transcription.getId().toString())
            .queryParam("is_current", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/transcriptions/transcription_workflow/expectedCurrentWorkflowResponse.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNotFound() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .queryParam("transcription_id", "-100")
            .queryParam("is_current", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();

        JSONAssert.assertEquals("[]", actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionMissingMandatoryFieldShouldThrow400Error() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = """
            {"title":"Bad Request","status":400,"detail":"Required request parameter 'transcription_id' for method parameter type Integer is not present"}
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionWorkflowAsNonSuperUserShouldThrow403Error() throws Exception {
        superAdminUserStub.givenUserIsNotAuthorised(mockUserIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .queryParam("transcription_id", "-100")
            .queryParam("is_current", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private void createWorkflowEntity(TranscriptionEntity transcription,
                                                             OffsetDateTime workflowTimestamp,
                                                             TranscriptionStatusEntity status,
                                                             String comment) {

        TranscriptionWorkflowEntity workflowEntity = new TranscriptionWorkflowEntity();
        workflowEntity.setTranscription(transcription);
        workflowEntity.setWorkflowActor(transcription.getCreatedBy());
        workflowEntity.setWorkflowTimestamp(workflowTimestamp);
        workflowEntity.setTranscriptionStatus(status);

        dartsDatabase.save(workflowEntity);

        addCommentToWorkflow(workflowEntity, comment, transcription.getCreatedBy());
    }

    private void addCommentToWorkflow(TranscriptionWorkflowEntity workflowEntity, String comment, UserAccountEntity userAccount) {
        TranscriptionCommentEntity commentEntity = new TranscriptionCommentEntity();
        commentEntity.setTranscription(workflowEntity.getTranscription());
        commentEntity.setTranscriptionWorkflow(workflowEntity);
        commentEntity.setComment(comment);
        commentEntity.setCommentTimestamp(workflowEntity.getWorkflowTimestamp());
        commentEntity.setAuthorUserId(userAccount.getId());
        commentEntity.setLastModifiedBy(userAccount);
        commentEntity.setCreatedBy(userAccount);
        dartsDatabase.save(commentEntity);
    }
}
