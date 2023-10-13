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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerUpdateTranscriptionWithTranscriberIntTest extends IntegrationBase {

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
    @Transactional
    void beforeEach() {
        authorisationStub.givenTestSchema();

        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        TranscriptionStub transcriptionStub = dartsDatabaseStub.getTranscriptionStub();
        TranscriptionStatusEntity approvedTranscriptionStatus = transcriptionStub.getTranscriptionStatusByEnum(APPROVED);

        TranscriptionWorkflowEntity approvedTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedBy(),
            transcriptionEntity.getCreatedDateTime().plusHours(1),
            approvedTranscriptionStatus,
            null
        );

        transcriptionEntity.getTranscriptionWorkflowEntities().add(approvedTranscriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(approvedTranscriptionStatus);
        dartsDatabaseStub.getTranscriptionRepository().save(transcriptionEntity);

        assertEquals(APPROVED.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(3, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getEmailAddress()).thenReturn(testUser.getEmailAddress());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();
    }

    @Test
    @Transactional
    void updateTranscriptionWithTranscriberWithoutComment() throws Exception {

        UpdateTranscription updateTranscription = new UpdateTranscription();
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

        final TranscriptionEntity withTranscriberTranscriptionEntity = dartsDatabaseStub.getTranscriptionRepository()
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
        assertNull(transcriptionWorkflowEntity.getWorkflowComment());
        assertEquals(testUserId, transcriptionWorkflowEntity.getCreatedBy().getId());
        assertEquals(testUserId, transcriptionWorkflowEntity.getLastModifiedBy().getId());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());
    }

    @Test
    @Transactional
    void updateTranscriptionShouldReturnTranscriptionNotFoundError() throws Exception {
        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());

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
    }

    @Test
    @Transactional
    void updateTranscriptionShouldReturnTranscriptionWorkflowActionInvalidError() throws Exception {
        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(COMPLETE.getId());

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
    }

    @Test
    @Transactional
    void updateTranscriptionShouldReturnForbiddenError() throws Exception {

        UserAccountRepository userAccountRepository = dartsDatabaseStub.getUserAccountRepository();
        UserAccountEntity testUser = dartsDatabaseStub.getUserAccountRepository().findById(testUserId).orElseThrow();
        testUser.getSecurityGroupEntities().clear();
        userAccountRepository.save(testUser);

        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(WITH_TRANSCRIBER.getId());

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
    }

    @Test
    @Transactional
    void updateTranscriptionShouldReturnOkWhenTranscriberOnlyUser() throws Exception {

        UserAccountRepository userAccountRepository = dartsDatabaseStub.getUserAccountRepository();
        UserAccountEntity testUser = dartsDatabaseStub.getUserAccountRepository().findById(testUserId).orElseThrow();
        testUser.getSecurityGroupEntities().clear();
        userAccountRepository.save(testUser);

        testUser = dartsDatabaseStub.getUserAccountStub()
            .createTranscriptionCompanyUser(authorisationStub.getCourthouseEntity());
        assertEquals(1, testUser.getSecurityGroupEntities().size());

        UpdateTranscription updateTranscription = new UpdateTranscription();
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

        final TranscriptionEntity withTranscriberTranscriptionEntity = dartsDatabaseStub.getTranscriptionRepository()
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
        assertNull(transcriptionWorkflowEntity.getWorkflowComment());
        assertEquals(testUserId, transcriptionWorkflowEntity.getCreatedBy().getId());
        assertEquals(testUserId, transcriptionWorkflowEntity.getLastModifiedBy().getId());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());
    }

}
