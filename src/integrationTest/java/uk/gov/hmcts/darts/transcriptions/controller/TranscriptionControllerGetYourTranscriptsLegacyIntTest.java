package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.net.URI;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

/**
 * Integration test for the TranscriptionController class, specifically for the "Get Your Transcripts" endpoint for legacy data.
 */
@AutoConfigureMockMvc
class TranscriptionControllerGetYourTranscriptsLegacyIntTest extends PostgresIntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions");

    @Autowired
    private MockMvc mockMvc;

    private UserAccountEntity testUser;
    private UserAccountEntity systemUser;

    @MockitoBean
    private CurrentTimeHelper currentTimeHelper;

    @BeforeEach
    void beforeEach() {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        systemUser = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
    }

    @Test
    void getYourTranscripts_ShouldReturnSingleWorkflow_WhenWorkflowHasBeenRevertedToRequested() throws Exception {
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().minimalTranscription();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionEntity);

        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T14:00:00Z"), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T14:00:00Z"), AWAITING_AUTHORISATION, transcriptionEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());
        var courtCase = transcriptionEntity.getCourtCase();

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number", is(courtCase.getCaseNumber())))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcriptionEntity.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-20T13:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].approved_ts").doesNotExist());

    }

    @Test
    void getYourTranscripts_ShouldReturnSingleWorkflow_WhenWorkflowHasBeenRevertedBackToApproved() throws Exception {
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().minimalTranscription();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T14:00:00Z"), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T15:00:00Z"), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T16:00:00Z"), APPROVED, transcriptionEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());
        var courtCase = transcriptionEntity.getCourtCase();

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number", is(courtCase.getCaseNumber())))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcriptionEntity.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Approved")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-20T13:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].approved_ts", is("2025-03-23T14:00:00Z")));

    }

    private void createTranscriptionWorkflow(UserAccountEntity userAccount, OffsetDateTime dateTime, TranscriptionStatusEnum transcriptionStatusEnum,
                                             TranscriptionEntity transcriptionEntity) {
        TranscriptionWorkflowEntity transcriptionWorkflowEntity =
            PersistableFactory.getTranscriptionWorkflowTestData().workflowForTranscriptionWithStatus(transcriptionEntity, transcriptionStatusEnum);
        transcriptionWorkflowEntity.setWorkflowActor(userAccount);
        transcriptionWorkflowEntity.setWorkflowTimestamp(dateTime);
        transcriptionEntity.getTranscriptionWorkflowEntities().add(transcriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(transcriptionWorkflowEntity.getTranscriptionStatus());
        dartsPersistence.save(transcriptionWorkflowEntity);
        dartsPersistence.save(transcriptionEntity);
    }

}