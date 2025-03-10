package uk.gov.hmcts.darts.transcriptions.controller;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.AUTHORISE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerUpdateTranscriptionApprovedIntTest extends IntegrationBase {

    @MockitoBean
    private Authorisation authorisation;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity mockUserIdentity;
    @MockitoBean
    private AuditApi mockAuditApi;

    private TranscriptionEntity transcriptionEntity;
    private UserAccountEntity testUser;

    private Integer transcriptionId;
    private Integer transcriptCreatorId;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        doNothing().when(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(SUPER_ADMIN));

        testUser = authorisationStub.getSeparateIntegrationUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        transcriptCreatorId = authorisationStub.getTestUser().getId();

        doNothing().when(mockAuditApi)
            .record(AUTHORISE_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionApprovedWithoutComment() throws Exception {

        TranscriptionEntity existingTranscription = dartsDatabase.getTranscriptionRepository().findById(
            transcriptionId).orElseThrow();
        CourthouseEntity courthouse = existingTranscription.getCourtCase().getCourthouse();
        dartsDatabase.getUserAccountStub().createTranscriptionCompanyUser(courthouse);
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(APPROVED.getId());

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

        verify(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER)
        );

        final TranscriptionEntity approvedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(APPROVED.getId(), approvedTranscriptionEntity.getTranscriptionStatus().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = approvedTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUser.getId(), transcriptionWorkflowEntity.getWorkflowActor().getId());

        List<NotificationEntity> notificationEntities = dartsDatabase.getNotificationRepository().findAll();
        List<String> templateList = notificationEntities.stream().map(NotificationEntity::getEventId).toList();
        assertTrue(templateList.contains("request_to_transcriber"));
        assertTrue(templateList.contains("transcription_request_approved"));

        verify(mockAuditApi).record(AUTHORISE_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionApprovedWithComment() throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(APPROVED.getId());
        updateTranscription.setWorkflowComment("APPROVED");

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

        verify(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER)
        );

        final TranscriptionEntity approvedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(APPROVED.getId(), approvedTranscriptionEntity.getTranscriptionStatus().getId());
        assertEquals(transcriptCreatorId, approvedTranscriptionEntity.getCreatedBy().getId());
        assertEquals(transcriptCreatorId, approvedTranscriptionEntity.getLastModifiedBy().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = approvedTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(
            APPROVED.toString(),
            dartsDatabase.getTranscriptionCommentRepository().findAll().get(0).getComment()
        );
        assertEquals(testUser.getId(), transcriptionWorkflowEntity.getWorkflowActor().getId());

        verify(mockAuditApi).record(AUTHORISE_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(APPROVED.getId());
        updateTranscription.setWorkflowComment("APPROVED");

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

        verify(authorisation).authoriseByTranscriptionId(
            -1, Set.of(APPROVER, TRANSCRIBER)
        );
        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscriptionShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());
        updateTranscription.setWorkflowComment("APPROVED");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"TRANSCRIPTION_105","title":"Transcription workflow action is not permitted","status":409}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

        verify(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER)
        );
        verifyNoInteractions(mockAuditApi);
    }


    @Test
    void givenAUpdateTranscriptionRequest_WhenRequestorIsSameAsApprover_ThenErrorIsReturned() throws Exception {
        //Test user is creating the transcription in base class
        testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        transcriptCreatorId = authorisationStub.getTestUser().getId();

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(APPROVED.getId());
        updateTranscription.setWorkflowComment("APPROVED");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();


        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"TRANSCRIPTION_114","title":"Transcription requestor cannot approve or reject their own transcription requests.","status":400}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

}
