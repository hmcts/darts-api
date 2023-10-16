package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerUpdateTranscriptionRejectedIntTest extends IntegrationBase {

    @MockBean
    private Authorisation authorisation;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    private Integer transcriptionId;
    private Integer testUserId;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        doNothing().when(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER));

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getEmailAddress()).thenReturn(testUser.getEmailAddress());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();
    }

    @Test
    @Transactional
    void updateTranscriptionRejectedWithoutCommentShouldReturnTranscriptionBadRequestError() throws Exception {

        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(REJECTED.getId());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions/%d", transcriptionId)))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscription));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"TRANSCRIPTION_103","title":"The workflow comment is required for this transcription update","status":400}
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

        verify(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER)
        );
    }

    @Test
    @Transactional
    void updateTranscriptionRejectedWithComment() throws Exception {

        UpdateTranscription updateTranscription = new UpdateTranscription();
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

        final TranscriptionEntity rejectedTranscriptionEntity = dartsDatabaseStub.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(REJECTED.getId(), rejectedTranscriptionEntity.getTranscriptionStatus().getId());
        assertEquals(testUserId, rejectedTranscriptionEntity.getCreatedBy().getId());
        assertEquals(testUserId, rejectedTranscriptionEntity.getLastModifiedBy().getId());
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
            dartsDatabaseStub.getTranscriptionCommentRepository().findAll().get(0).getComment()
        );
        assertEquals(testUserId, transcriptionWorkflowEntity.getCreatedBy().getId());
        assertEquals(testUserId, transcriptionWorkflowEntity.getLastModifiedBy().getId());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());
    }

    @Test
    @Transactional
    void updateTranscriptionShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(REJECTED.getId());
        updateTranscription.setWorkflowComment("REJECTED");

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
    }

    @Test
    @Transactional
    void updateTranscriptionShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());
        updateTranscription.setWorkflowComment("REJECTED");

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
    }

}
