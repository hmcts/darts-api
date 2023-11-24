package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.Problem;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptions;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsRequest;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsResponse;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TranscriptionControllerUpdateTranscriptionsTest extends IntegrationBase {

    @MockBean
    private Authorisation authorisation;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;
    @MockBean
    private AuditApi mockAuditApi;

    private TranscriptionEntity transcriptionEntity;

    private TranscriptionEntity transcriptionEntity1;

    private TranscriptionEntity transcriptionEntity2;

    private UserAccountEntity testUser;

    private Integer transcriptionId;

    private Integer transcriptionId1;

    private Integer transcriptionId2;

    private Integer testUserId;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        transcriptionEntity1 = authorisationStub.addNewTranscription();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity1.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity1.getTranscriptionWorkflowEntities().size());

        transcriptionId1 = transcriptionEntity1.getId();

        transcriptionEntity2 = authorisationStub.addNewTranscription();
        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionEntity2.getTranscriptionStatus().getId());
        assertEquals(2, transcriptionEntity2.getTranscriptionWorkflowEntities().size());

        transcriptionId2 = transcriptionEntity2.getId();

        doNothing().when(authorisation).authoriseByTranscriptionId(
            transcriptionId, Set.of(APPROVER, TRANSCRIBER));

        testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();
    }

    @Test
    public void testTransactionsUpdateHideSuccessWhereTransactionIdsAreFoundAndStateIsGood() throws Exception {
        TranscriptionEntity existingTranscription = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId).orElseThrow();
        TranscriptionStatusEntity entity = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.COMPLETE);
        existingTranscription.setTranscriptionStatus(entity);

        TranscriptionEntity existingTranscription1 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId1).orElseThrow();
        TranscriptionStatusEntity entity1 = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.REJECTED);
        existingTranscription1.setTranscriptionStatus(entity1);

        UpdateTranscriptionsRequest updateTranscriptions = new UpdateTranscriptionsRequest();
        updateTranscriptions.setTranscriptions(new ArrayList<UpdateTranscriptions>());
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(transcriptionId);
        transcriptions.setHideRequestFromRequestor(true);

        UpdateTranscriptions transcriptions1 = new UpdateTranscriptions();
        transcriptions1.setTranscriptionId(transcriptionId1);
        transcriptions1.setHideRequestFromRequestor(true);

        // we should be able to set hide false on a transcription without
        // a known state
        UpdateTranscriptions transcriptions2 = new UpdateTranscriptions();
        transcriptions2.setTranscriptionId(transcriptionId2);
        transcriptions2.setHideRequestFromRequestor(false);

        updateTranscriptions.getTranscriptions().add(transcriptions);
        updateTranscriptions.getTranscriptions().add(transcriptions1);
        updateTranscriptions.getTranscriptions().add(transcriptions2);

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions")))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscriptions));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        // we expect a success
        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        UpdateTranscriptionsResponse successResponse = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            UpdateTranscriptionsResponse.class);
        Assertions.assertNotNull(successResponse);
        Assertions.assertEquals(3, successResponse.getTranscriptions().size());
        Assertions.assertTrue(successResponse.getTranscriptions().containsAll(updateTranscriptions.getTranscriptions()));
        Assertions.assertTrue(existingTranscription.getHideRequestFromRequestor());
        Assertions.assertTrue(existingTranscription1.getHideRequestFromRequestor());

        TranscriptionEntity existingTranscription2 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId2).orElseThrow();

        Assertions.assertFalse(existingTranscription2.getHideRequestFromRequestor());
    }

    @Test
    public void testTransactionsUpdateHideSuccessWhereTransactionIdsAreFoundAndWorkflowStateIsGood() throws Exception {
        TranscriptionEntity existingTranscription = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId).orElseThrow();
        TranscriptionStatusEntity entity = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.COMPLETE);
        existingTranscription.setTranscriptionStatus(entity);

        // map the transcription state to the workflow state
        TranscriptionEntity existingTranscription1 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId1).orElseThrow();
        TranscriptionStatusEntity status = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.REJECTED);

        TranscriptionWorkflowEntity workflowBEntity = new TranscriptionWorkflowEntity();
        workflowBEntity.setTranscription(existingTranscription1);
        workflowBEntity.setWorkflowActor(existingTranscription1.getCreatedBy());
        workflowBEntity.setWorkflowTimestamp(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        workflowBEntity.setTranscriptionStatus(status);
        existingTranscription1.getTranscriptionWorkflowEntities().add(workflowBEntity);
        dartsDatabase.save(workflowBEntity);

        UpdateTranscriptionsRequest updateTranscriptions = new UpdateTranscriptionsRequest();
        updateTranscriptions.setTranscriptions(new ArrayList<UpdateTranscriptions>());
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(transcriptionId);
        transcriptions.setHideRequestFromRequestor(true);

        UpdateTranscriptions transcriptions1 = new UpdateTranscriptions();
        transcriptions1.setTranscriptionId(transcriptionId1);
        transcriptions1.setHideRequestFromRequestor(true);

        // we should be able to set hide false on a transcription without
        // a known state
        UpdateTranscriptions transcriptions2 = new UpdateTranscriptions();
        transcriptions2.setTranscriptionId(transcriptionId2);
        transcriptions2.setHideRequestFromRequestor(false);

        updateTranscriptions.getTranscriptions().add(transcriptions);
        updateTranscriptions.getTranscriptions().add(transcriptions1);
        updateTranscriptions.getTranscriptions().add(transcriptions2);

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions")))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscriptions));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        // we expect a success
        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        UpdateTranscriptionsResponse successResponse = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            UpdateTranscriptionsResponse.class);
        Assertions.assertNotNull(successResponse);
        Assertions.assertEquals(3, successResponse.getTranscriptions().size());
        Assertions.assertTrue(successResponse.getTranscriptions().containsAll(updateTranscriptions.getTranscriptions()));
        Assertions.assertTrue(existingTranscription.getHideRequestFromRequestor());
        Assertions.assertTrue(existingTranscription1.getHideRequestFromRequestor());

        TranscriptionEntity existingTranscription2 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId2).orElseThrow();

        Assertions.assertFalse(existingTranscription2.getHideRequestFromRequestor());
    }

    @Test
    public void testTransactionsUpdateHidePartialFailureWhereSomeTransactionIdsAreUpdatedAndSomeNotFound() throws Exception {
        TranscriptionEntity existingTranscription = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId).orElseThrow();
        TranscriptionStatusEntity entity = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.COMPLETE);
        existingTranscription.setTranscriptionStatus(entity);

        TranscriptionEntity existingTranscription1 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId1).orElseThrow();
        TranscriptionStatusEntity entity1 = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.REJECTED);
        existingTranscription1.setTranscriptionStatus(entity1);

        UpdateTranscriptionsRequest updateTranscriptions = new UpdateTranscriptionsRequest();
        updateTranscriptions.setTranscriptions(new ArrayList<UpdateTranscriptions>());
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(transcriptionId);
        transcriptions.setHideRequestFromRequestor(true);

        UpdateTranscriptions transcriptions1 = new UpdateTranscriptions();
        transcriptions1.setTranscriptionId(transcriptionId1);
        transcriptions1.setHideRequestFromRequestor(true);

        // force a failure on this record as the transcription will not be found
        Integer idThatDoesNotExist = 100;
        UpdateTranscriptions transcriptions2 = new UpdateTranscriptions();
        transcriptions2.setTranscriptionId(idThatDoesNotExist);
        transcriptions2.setHideRequestFromRequestor(false);

        updateTranscriptions.getTranscriptions().add(transcriptions);
        updateTranscriptions.getTranscriptions().add(transcriptions1);
        updateTranscriptions.getTranscriptions().add(transcriptions2);

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions")))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscriptions));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().is4xxClientError())
            .andReturn();

        // assert a partial failure
        Assertions.assertEquals(400, mvcResult.getResponse().getStatus());
        Problem failureResponse = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
            mvcResult.getResponse().getContentAsString(),
            Problem.class);

        // assert the failure response
        Assertions.assertNotNull(failureResponse);
        String partialFailure = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.partial_failure");
        UpdateTranscriptionsResponse partialFailureResponse = objectMapper.readValue(
            partialFailure,
            UpdateTranscriptionsResponse.class);
        Assertions.assertEquals(1, partialFailureResponse.getTranscriptions().size());
        Assertions.assertEquals(idThatDoesNotExist, partialFailureResponse.getTranscriptions().get(0).getTranscriptionId());

        // assert partial success i.e. that the hide flags in the database have been set
        Assertions.assertTrue(existingTranscription.getHideRequestFromRequestor());
        Assertions.assertTrue(existingTranscription1.getHideRequestFromRequestor());
    }

    @Test
    public void testTransactionsUpdateHidePartialFailureWhereSomeTransactionIdsAreUpdatedAndSomeAreNotInCorrectStateForHide() throws Exception {
        TranscriptionEntity existingTranscription = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId).orElseThrow();
        TranscriptionStatusEntity entity = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.COMPLETE);
        existingTranscription.setTranscriptionStatus(entity);

        // set an incorrect state for a hide to take place
        TranscriptionEntity existingTranscription1 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId1).orElseThrow();
        TranscriptionStatusEntity entity1 = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.APPROVED);
        existingTranscription1.setTranscriptionStatus(entity1);

        UpdateTranscriptionsRequest updateTranscriptions = new UpdateTranscriptionsRequest();
        updateTranscriptions.setTranscriptions(new ArrayList<UpdateTranscriptions>());
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(transcriptionId);
        transcriptions.setHideRequestFromRequestor(true);

        UpdateTranscriptions transcriptions1 = new UpdateTranscriptions();
        transcriptions1.setTranscriptionId(transcriptionId1);
        transcriptions1.setHideRequestFromRequestor(true);

        updateTranscriptions.getTranscriptions().add(transcriptions);
        updateTranscriptions.getTranscriptions().add(transcriptions1);

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions")))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscriptions));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().is4xxClientError())
            .andReturn();

        Assertions.assertEquals(400, mvcResult.getResponse().getStatus());
        Problem failureResponse = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
            mvcResult.getResponse().getContentAsString(),
            Problem.class);

        // assert the failure response
        Assertions.assertNotNull(failureResponse);
        String partialFailure = JsonPath.parse(mvcResult.getResponse().getContentAsString())
            .read("$.partial_failure");
        UpdateTranscriptionsResponse partialFailureResponse = objectMapper.readValue(
            partialFailure,
            UpdateTranscriptionsResponse.class);
        Assertions.assertEquals(1, partialFailureResponse.getTranscriptions().size());
        Assertions.assertEquals(transcriptionId1, partialFailureResponse.getTranscriptions().get(0).getTranscriptionId());

        // assert partial success in the database
        Assertions.assertTrue(existingTranscription.getHideRequestFromRequestor());
        Assertions.assertFalse(existingTranscription1.getHideRequestFromRequestor());
    }

    @Test
    public void testSuccessChangeFromHiddenToUnhiddenRegardlessOfTranscriptionState() throws Exception {
        TranscriptionEntity existingTranscription = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId).orElseThrow();
        TranscriptionStatusEntity entity = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.COMPLETE);
        existingTranscription.setTranscriptionStatus(entity);

        TranscriptionEntity existingTranscription1 = dartsDatabaseStub.getTranscriptionRepository().findById(
            transcriptionId1).orElseThrow();
        TranscriptionStatusEntity entity1 = transcriptionStub.getTranscriptionStatusByEnum(TranscriptionStatusEnum.REJECTED);
        existingTranscription1.setTranscriptionStatus(entity1);

        UpdateTranscriptionsRequest updateTranscriptions = new UpdateTranscriptionsRequest();
        updateTranscriptions.setTranscriptions(new ArrayList<UpdateTranscriptions>());
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(transcriptionId);
        transcriptions.setHideRequestFromRequestor(true);

        UpdateTranscriptions transcriptions1 = new UpdateTranscriptions();
        transcriptions1.setTranscriptionId(transcriptionId1);
        transcriptions1.setHideRequestFromRequestor(true);

        updateTranscriptions.getTranscriptions().add(transcriptions);
        updateTranscriptions.getTranscriptions().add(transcriptions1);

        // set the flag to be on
        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/transcriptions")))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscriptions));
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertTrue(existingTranscription.getHideRequestFromRequestor());
        Assertions.assertTrue(existingTranscription1.getHideRequestFromRequestor());

        existingTranscription1.setTranscriptionStatus(transcriptionStub.getTranscriptionStatusByEnum(AWAITING_AUTHORISATION));

        updateTranscriptions = new UpdateTranscriptionsRequest();
        updateTranscriptions.setTranscriptions(new ArrayList<UpdateTranscriptions>());
        transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(transcriptionId1);
        transcriptions.setHideRequestFromRequestor(false);
        updateTranscriptions.getTranscriptions().add(transcriptions);

        requestBuilder = patch(URI.create(
            String.format("/transcriptions")))
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(updateTranscriptions));
        mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());
        Assertions.assertFalse(existingTranscription1.getHideRequestFromRequestor());
    }
}
