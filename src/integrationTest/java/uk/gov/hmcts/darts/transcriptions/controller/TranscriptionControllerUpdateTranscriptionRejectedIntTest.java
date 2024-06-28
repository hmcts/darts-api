package uk.gov.hmcts.darts.transcriptions.controller;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REJECT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerUpdateTranscriptionRejectedIntTest extends IntegrationBase {

    @MockBean
    private Authorisation authorisation;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;
    @MockBean
    private AuditApi mockAuditApi;

    private TranscriptionEntity transcriptionEntity;
    private UserAccountEntity testUser;

    private Integer transcriptionId;
    private Integer testUserId;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        doNothing().when(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER));

        testUser = authorisationStub.getSeparateIntegrationUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();

        doNothing().when(mockAuditApi)
            .record(REJECT_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionRejectedWithoutCommentShouldReturnTranscriptionBadRequestError() throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(REJECTED.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("TRANSCRIPTION_103")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("The workflow comment is required for this transcription update")));

        verify(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER)
        );
        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscriptionRejectedWithComment() throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(REJECTED.getId());
        updateTranscription.setWorkflowComment("REJECTED");

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

        final TranscriptionEntity rejectedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(REJECTED.getId(), rejectedTranscriptionEntity.getTranscriptionStatus().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = rejectedTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(
            REJECTED.toString(),
            dartsDatabase.getTranscriptionCommentRepository().findAll().get(0).getComment()
        );
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        verify(mockAuditApi).record(REJECT_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(REJECTED.getId());
        updateTranscription.setWorkflowComment("REJECTED");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", -1)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("TRANSCRIPTION_101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(404)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("The requested transcription cannot be found")));

        verify(authorisation).authoriseByTranscriptionId(
            -1, Set.of(APPROVER, TRANSCRIBER)
        );
        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscriptionShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());
        updateTranscription.setWorkflowComment("REJECTED");

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("TRANSCRIPTION_105")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(409)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("Transcription workflow action is not permitted")));

        verify(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER)
        );
        verifyNoInteractions(mockAuditApi);
    }
}