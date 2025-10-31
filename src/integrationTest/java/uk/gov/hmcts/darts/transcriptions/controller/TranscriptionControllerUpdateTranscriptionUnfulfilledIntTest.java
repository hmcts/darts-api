package uk.gov.hmcts.darts.transcriptions.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UNFULFILLED_TRANSCRIPTION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.UNFULFILLED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
class TranscriptionControllerUpdateTranscriptionUnfulfilledIntTest extends IntegrationBase {

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity mockUserIdentity;
    @MockitoBean
    private AuditApi mockAuditApi;
    @MockitoBean
    private NotificationApi notificationApi;

    private Long transcriptionId;
    private Integer testUserId;
    private UserAccountEntity testUser;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        TranscriptionStub transcriptionStub = dartsDatabase.getTranscriptionStub();
        TranscriptionStatusEntity withTranscriberTranscriptionStatus = transcriptionStub.getTranscriptionStatusByEnum(WITH_TRANSCRIBER);

        TranscriptionWorkflowEntity withTranscriberTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedById(),
            transcriptionEntity.getCreatedDateTime().plusHours(1),
            withTranscriberTranscriptionStatus
        );

        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        transcriptionEntity.getTranscriptionWorkflowEntities().add(withTranscriberTranscriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(withTranscriberTranscriptionStatus);
        dartsDatabase.getTranscriptionRepository().save(transcriptionEntity);

        assertEquals(WITH_TRANSCRIBER.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(3, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();

        doNothing().when(mockAuditApi)
            .record(UNFULFILLED_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @ParameterizedTest
    @CsvSource({
        "UNFULFILLED REASON: INAUDIBLE",
        "UNFULFILLED REASON: NO AUDIO / WHITE NOISE",
        "UNFULFILLED REASON: 1 SECOND AUDIO",
        "UNFULFILLED REASON: OTHER - USER ENTERED COMMENTS"
    })
    void updateTranscription_ShouldReturnOk_WithTranscriptionComment(String transcriptionComment) throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(UNFULFILLED.getId());
        updateTranscription.setWorkflowComment(transcriptionComment);

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionWorkflowId = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.transcription_workflow_id");
        assertNotNull(transcriptionWorkflowId);

        final TranscriptionEntity unfulfilledTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(UNFULFILLED.getId(), unfulfilledTranscriptionEntity.getTranscriptionStatus().getId());
        assertEquals(testUserId, unfulfilledTranscriptionEntity.getCreatedById());
        assertEquals(testUserId, unfulfilledTranscriptionEntity.getLastModifiedById());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = unfulfilledTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(1, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        verify(notificationApi).scheduleNotification(any());
        verify(mockAuditApi).record(UNFULFILLED_TRANSCRIPTION, testUser, unfulfilledTranscriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscription_ShouldReturnTranscriptionUnprocessableEntityError_WhereTranscriptionCommentIsNull() throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(UNFULFILLED.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"TRANSCRIPTION_103","title":"The workflow comment is required for this transcription update","status":422}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
        verifyNoInteractions(notificationApi);
        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscription_ShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(UNFULFILLED.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", -1)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"TRANSCRIPTION_101","title":"The requested transcription cannot be found","status":404}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
        verifyNoInteractions(notificationApi);
        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscription_ShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(UNFULFILLED.getId());
        updateTranscription.setWorkflowComment("test comment");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        UpdateTranscriptionRequest updateTranscription2 = new UpdateTranscriptionRequest();
        updateTranscription2.setTranscriptionStatusId(CLOSED.getId());
        updateTranscription2.setWorkflowComment("test comment");

        MockHttpServletRequestBuilder requestBuilder2 = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder2)
            .andExpect(status().isConflict())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"TRANSCRIPTION_105","title":"Transcription workflow action is not permitted","status":409}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void updateTranscription_ShouldReturnForbiddenError() throws Exception {

        UserAccountRepository userAccountRepository = dartsDatabase.getUserAccountRepository();
        UserAccountEntity testUser = dartsDatabase.getUserAccountRepository().findById(testUserId).orElseThrow();
        testUser.getSecurityGroupEntities().clear();
        userAccountRepository.save(testUser);

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(UNFULFILLED.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"AUTHORISATION_100","title":"User is not authorised for the associated courthouse","status":403}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(notificationApi);
        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscription_ShouldReturnOk_WhenTranscriberOnlyUser() throws Exception {

        UserAccountRepository userAccountRepository = dartsDatabase.getUserAccountRepository();
        UserAccountEntity testUser = dartsDatabase.getUserAccountRepository().findById(testUserId).orElseThrow();
        testUser.getSecurityGroupEntities().clear();
        userAccountRepository.save(testUser);

        testUser = dartsDatabase.getUserAccountStub()
            .createTranscriptionCompanyUser(authorisationStub.getCourthouseEntity());
        assertEquals(1, testUser.getSecurityGroupEntities().size());

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(UNFULFILLED.getId());
        updateTranscription.setWorkflowComment("test comment");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionWorkflowId = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.transcription_workflow_id");
        assertNotNull(transcriptionWorkflowId);

        final TranscriptionEntity withTranscriberTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(UNFULFILLED.getId(), withTranscriberTranscriptionEntity.getTranscriptionStatus().getId());
        assertEquals(testUserId, withTranscriberTranscriptionEntity.getCreatedById());
        assertEquals(testUserId, withTranscriberTranscriptionEntity.getLastModifiedById());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = withTranscriberTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(1, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        verify(notificationApi).scheduleNotification(any());

        verify(mockAuditApi).record(UNFULFILLED_TRANSCRIPTION, testUser, withTranscriberTranscriptionEntity.getCourtCase());

    }

}
