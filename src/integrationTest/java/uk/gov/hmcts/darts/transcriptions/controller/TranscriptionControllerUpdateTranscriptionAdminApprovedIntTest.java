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
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.AUTHORISE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerUpdateTranscriptionAdminApprovedIntTest extends IntegrationBase {

    public static final String ENDPOINT_URL = "/admin/transcriptions/%d";
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

    private Integer transcriptionId;
    private Integer transcriptCreatorId;
    private UserAccountEntity testUser;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        doNothing().when(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(SUPER_ADMIN));

        testUser = dartsDatabase.getUserAccountStub().createSuperAdminUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        when(mockUserIdentity.userHasGlobalAccess(any())).thenReturn(true);
        transcriptCreatorId = authorisationStub.getTestUser().getId();

        doNothing().when(mockAuditApi)
            .record(AUTHORISE_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionAdmin_ShouldUpdateAwaitingAuthorisationTranscriptionToRequested() throws Exception {

        dartsDatabase.getUserAccountStub().createSuperAdminUser();
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(REQUESTED.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format(ENDPOINT_URL, transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        Integer transcriptionWorkflowId = JsonPath.parse(response)
            .read("$.transcription_status_id");
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionWorkflowId);
    }

    @Test
    void updateTranscriptionAdmin_ShouldSetAwaitingAuthorisationTranscriptionToRequestedWithComment() throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(REQUESTED.getId());
        updateTranscription.setWorkflowComment("the comment");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format(ENDPOINT_URL, transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        Integer transcriptionStatusId = JsonPath.parse(mvcResult.getResponse().getContentAsString()).read("$.transcription_status_id");
        assertNotNull(transcriptionStatusId);

        final TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(transcriptCreatorId, transcriptionEntity.getCreatedBy().getId());
        assertEquals(transcriptCreatorId, transcriptionEntity.getLastModifiedById());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = transcriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionWorkflowEntity.getTranscriptionStatus().getId());
        assertEquals("the comment", dartsDatabase.getTranscriptionCommentRepository().findAll().getFirst().getComment());
        assertEquals(testUser.getId(), transcriptionWorkflowEntity.getWorkflowActor().getId());
    }

    @Test
    void updateTranscriptionAdmin_ShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(APPROVED.getId());
        updateTranscription.setWorkflowComment("APPROVED");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format(ENDPOINT_URL, -1)))
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
    }

    @Test
    void updateTranscriptionAdmin_ShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());
        updateTranscription.setWorkflowComment("APPROVED");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format(ENDPOINT_URL, transcriptionId)))
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

    }

}
