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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.ACCEPT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerUpdateTranscriptionWithTranscriberIntTest extends IntegrationBase {

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

        TranscriptionStub transcriptionStub = dartsDatabase.getTranscriptionStub();
        TranscriptionStatusEntity approvedTranscriptionStatus = transcriptionStub.getTranscriptionStatusByEnum(APPROVED);

        TranscriptionWorkflowEntity approvedTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedBy(),
            transcriptionEntity.getCreatedDateTime().plusHours(1),
            approvedTranscriptionStatus
        );

        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        transcriptionEntity.getTranscriptionWorkflowEntities().add(approvedTranscriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(approvedTranscriptionStatus);
        dartsDatabase.getTranscriptionRepository().save(transcriptionEntity);

        assertEquals(APPROVED.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(3, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();

        doNothing().when(mockAuditApi)
            .record(ACCEPT_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionWithTranscriberWithoutComment() throws Exception {

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());

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
        assertEquals(WITH_TRANSCRIBER.getId(), withTranscriberTranscriptionEntity.getTranscriptionStatus().getId());
        assertEquals(testUserId, withTranscriberTranscriptionEntity.getCreatedBy().getId());
        assertEquals(testUserId, withTranscriberTranscriptionEntity.getLastModifiedBy().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = withTranscriberTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        verify(mockAuditApi).record(ACCEPT_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void updateTranscriptionShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", -1)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("TRANSCRIPTION_101")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(404)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("The requested transcription cannot be found")));

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscriptionShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(COMPLETE.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("TRANSCRIPTION_105")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(409)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("Transcription workflow action is not permitted")));

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscriptionShouldReturnForbiddenError() throws Exception {

        UserAccountRepository userAccountRepository = dartsDatabase.getUserAccountRepository();
        UserAccountEntity testUser = dartsDatabase.getUserAccountRepository().findById(testUserId).orElseThrow();
        testUser.getSecurityGroupEntities().clear();
        userAccountRepository.save(testUser);

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
       mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("AUTHORISATION_100")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(403)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("User is not authorised for the associated courthouse")));

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void updateTranscriptionShouldReturnOkWhenTranscriberOnlyUser() throws Exception {

        UserAccountRepository userAccountRepository = dartsDatabase.getUserAccountRepository();
        UserAccountEntity testUser = dartsDatabase.getUserAccountRepository().findById(testUserId).orElseThrow();
        testUser.getSecurityGroupEntities().clear();
        userAccountRepository.save(testUser);

        testUser = dartsDatabase.getUserAccountStub()
            .createTranscriptionCompanyUser(authorisationStub.getCourthouseEntity());
        assertEquals(1, testUser.getSecurityGroupEntities().size());

        UpdateTranscriptionRequest updateTranscription = new UpdateTranscriptionRequest();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());

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
        assertEquals(WITH_TRANSCRIBER.getId(), withTranscriberTranscriptionEntity.getTranscriptionStatus().getId());
        assertEquals(testUserId, withTranscriberTranscriptionEntity.getCreatedBy().getId());
        assertEquals(testUserId, withTranscriberTranscriptionEntity.getLastModifiedBy().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = withTranscriberTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            updateTranscription.getTranscriptionStatusId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        verify(mockAuditApi).record(ACCEPT_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

}