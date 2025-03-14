package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class TranscriptionControllerGetTranscriptionTest extends IntegrationBase {

    private static final String ENDPOINT_URL_TRANSCRIPTION = "/transcriptions/{transcription_id}";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final List<String> TAGS_TO_IGNORE = List.of("case_id", "hearing_id", "transcription_id");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @BeforeEach
    void setUp() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourthouseEntity courthouseEntity = hearingEntity.getCourtroom().getCourthouse();
        assertEquals(SOME_COURTHOUSE.toUpperCase(Locale.ROOT), courthouseEntity.getCourthouseName());

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(courthouseEntity);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void getTranscriptionWithHearing() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);

        UserAccountEntity userAccount = transcription.getCreatedBy();

        addTranscriptionWorkflow(transcription, userAccount, "comment1", TranscriptionStatusEnum.REQUESTED);
        addTranscriptionWorkflow(transcription, userAccount, "comment2", TranscriptionStatusEnum.APPROVED);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        String expected = TestUtils.removeTags(
            TAGS_TO_IGNORE,
            getContentsFromFile(
                "tests/transcriptions/transcription/expectedResponse.json")
                .replace("$COURTHOUSE_ID", hearingEntity.getCourtroom().getCourthouse().getId().toString())
                .replace("$USER_ACCOUNT_ID", userAccount.getId().toString())
        );
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    void getTranscriptionWithHiddenDocumentReturnsNotFound() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        UserAccountEntity userAccount = hearingEntity.getCreatedBy();

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            userAccount, hearingEntity.getCourtCase(), hearingEntity, SOME_DATE_TIME, true
        );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    @Test
    void getTranscriptionWithHiddenDocumentCanBeSeenBySuperAdmin() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        UserAccountEntity userAccount = hearingEntity.getCreatedBy();

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            userAccount, hearingEntity.getCourtCase(), hearingEntity, SOME_DATE_TIME, true
        );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        mockMvc.perform(requestBuilder).andExpect(status().isOk());
    }

    @Test
    void getTranscriptionWithLegacyComments() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);


        UserAccountEntity userAccount = transcription.getCreatedBy();

        addComment(transcription, null, "comment1", userAccount);
        addComment(transcription, null, "comment2", userAccount);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        String expected = TestUtils.removeTags(
            TAGS_TO_IGNORE,
            getContentsFromFile(
                "tests/transcriptions/transcription/expectedResponseWithLegacyComment.json")
                .replace("$COURTHOUSE_ID", hearingEntity.getCourtroom().getCourthouse().getId().toString())
                .replace("$USER_ACCOUNT_ID", userAccount.getId().toString())
        );
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNoHearing() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity.getCourtroom());

        transcription.getCourtCases().add(hearingEntity.getCourtCase());
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);

        UserAccountEntity userAccount = transcription.getCreatedBy();

        addTranscriptionWorkflow(transcription, userAccount, "comment1", TranscriptionStatusEnum.REQUESTED);
        addTranscriptionWorkflow(transcription, userAccount, "comment2", TranscriptionStatusEnum.APPROVED);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        String expected = TestUtils.removeTags(
            TAGS_TO_IGNORE,
            getContentsFromFile(
                "tests/transcriptions/transcription/expectedResponseNoHearing.json")
                .replace("$COURTHOUSE_ID", hearingEntity.getCourtroom().getCourthouse().getId().toString())
                .replace("$USER_ACCOUNT_ID", userAccount.getId().toString())
        );
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNoHearingOrCourtroom() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription((CourtroomEntity) null);

        transcription.getCourtCases().add(hearingEntity.getCourtCase());
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);

        UserAccountEntity userAccount = transcription.getCreatedBy();

        addTranscriptionWorkflow(transcription, userAccount, "comment1", TranscriptionStatusEnum.REQUESTED);
        addTranscriptionWorkflow(transcription, userAccount, "comment2", TranscriptionStatusEnum.APPROVED);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        String expected = TestUtils.removeTags(
            TAGS_TO_IGNORE,
            getContentsFromFile(
                "tests/transcriptions/transcription/expectedResponseNoCourtroom.json")
                .replace("$COURTHOUSE_ID", hearingEntity.getCourtroom().getCourthouse().getId().toString())
                .replace("$USER_ACCOUNT_ID", userAccount.getId().toString())
        );
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNoUrgency() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity, false);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);

        UserAccountEntity userAccount = transcription.getCreatedBy();

        addTranscriptionWorkflow(transcription, userAccount, "comment1", TranscriptionStatusEnum.REQUESTED);
        addTranscriptionWorkflow(transcription, userAccount, "comment2", TranscriptionStatusEnum.APPROVED);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        String expected = TestUtils.removeTags(
            TAGS_TO_IGNORE,
            getContentsFromFile(
                "tests/transcriptions/transcription/expectedResponseNoUrgency.json")
                .replace("$COURTHOUSE_ID", hearingEntity.getCourtroom().getCourthouse().getId().toString())
                .replace("$USER_ACCOUNT_ID", userAccount.getId().toString())
        );
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNotFoundWhenIsCurrentFalse() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription.setIsCurrent(false);
        transcription = dartsDatabase.save(transcription);

        UserAccountEntity userAccount = transcription.getCreatedBy();

        addTranscriptionWorkflow(transcription, userAccount, "comment1", TranscriptionStatusEnum.REQUESTED);
        addTranscriptionWorkflow(transcription, userAccount, "comment2", TranscriptionStatusEnum.APPROVED);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription/expectedResponseNotFound.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, -999);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription/expectedResponseNotFound.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private void addCommentToWorkflow(TranscriptionWorkflowEntity workflowEntity, String comment, UserAccountEntity userAccount) {
        addComment(workflowEntity.getTranscription(), workflowEntity, comment, userAccount);
    }

    private void addComment(TranscriptionEntity transcriptionEntity,
                            TranscriptionWorkflowEntity workflowEntity,
                            String comment, UserAccountEntity userAccount) {
        TranscriptionCommentEntity commentEntity = new TranscriptionCommentEntity();
        commentEntity.setTranscription(transcriptionEntity);
        commentEntity.setTranscriptionWorkflow(workflowEntity);
        commentEntity.setComment(comment);
        commentEntity.setLastModifiedBy(userAccount);
        commentEntity.setCreatedBy(userAccount);
        dartsDatabase.save(commentEntity);
    }

    private void addTranscriptionWorkflow(TranscriptionEntity transcription, UserAccountEntity userAccount, String comment, TranscriptionStatusEnum status) {
        TranscriptionWorkflowEntity workflowAEntity = new TranscriptionWorkflowEntity();
        workflowAEntity.setTranscription(transcription);
        workflowAEntity.setWorkflowActor(transcription.getCreatedBy());
        workflowAEntity.setWorkflowTimestamp(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        workflowAEntity.setTranscriptionStatus(dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(status));
        dartsDatabase.save(workflowAEntity);

        addCommentToWorkflow(workflowAEntity, comment, userAccount);
    }
}
