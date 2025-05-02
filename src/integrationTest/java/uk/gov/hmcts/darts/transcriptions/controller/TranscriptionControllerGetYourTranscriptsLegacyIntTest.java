package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

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
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TranscriptionUrgencyRepository transcriptionUrgencyRepository;
    @Autowired
    private SecurityGroupRepository securityGroupRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

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
    void getYourTranscripts_ShouldReturnRequesterTranscriptions_WhenNoLinkedHearing_WithTranscriptionUrgency() throws Exception {
        // creates a transcription for a different user that should not be returned
        var transcriptionForOtherUser = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(systemUser)
            .createdById(systemUser.getId())
            .lastModifiedById(systemUser.getId())
            .build().getEntity();
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), REQUESTED, transcriptionForOtherUser);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionForOtherUser);

        TranscriptionUrgencyEntity urgencyEntity = transcriptionUrgencyRepository.findById(TranscriptionUrgencyEnum.STANDARD.getId()).orElseThrow();
        // Creates a transcription without a linked hearing
        var transcriptionByRequester = PersistableFactory.getTranscriptionTestData().minimalTranscription();
        transcriptionByRequester.setTranscriptionUrgency(urgencyEntity);

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, transcriptionByRequester);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionByRequester);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());

        var courtCase = transcriptionByRequester.getCourtCase();

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionByRequester.getId().intValue())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number", is(courtCase.getCaseNumber())))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name",
                                is(transcriptionByRequester.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id",
                                is(TranscriptionUrgencyEnum.STANDARD.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description", is(urgencyEntity.getDescription())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order", is(urgencyEntity.getPriorityOrder())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscripts_ShouldReturnRequesterTranscriptions_WhenNoLinkedHearing_WithNoTranscriptionUrgency() throws Exception {
        // creates a transcription for a different user that should not be returned
        var transcriptionForOtherUser = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(systemUser)
            .createdById(systemUser.getId())
            .lastModifiedById(systemUser.getId())
            .build().getEntity();
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), REQUESTED, transcriptionForOtherUser);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionForOtherUser);

        var transcriptionByRequester1 = PersistableFactory.getTranscriptionTestData().minimalTranscription();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, transcriptionByRequester1);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionByRequester1);

        var transcriptionByRequester2 = PersistableFactory.getTranscriptionTestData().minimalTranscription();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-24T09:00:00Z"), REQUESTED, transcriptionByRequester2);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-24T09:00:00Z"), AWAITING_AUTHORISATION, transcriptionByRequester2);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());
        requestBuilder.content("");

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(2)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionByRequester2.getId().intValue())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-24T09:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].urgency").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order").doesNotExist())

            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_id", is(transcriptionByRequester1.getId().intValue())))
            .andExpect(jsonPath("$.requester_transcriptions[1].requested_ts", is("2025-03-20T13:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[1].hearing_date").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[1].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[1].urgency").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_urgency.priority_order").doesNotExist())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Disabled("This test should pass so a ticket has been raised to fix it - DMP-5053")
    @Test
    void getYourTranscripts_ShouldReturnRequesterTranscriptions_WithLinkedHearingButNoCourtCase() throws Exception {
        var hearing = PersistableFactory.getHearingTestData().someMinimalBuilder()
            .hearingDate(LocalDate.of(2025, 3, 19))
            .build().getEntity();
        // creates a transcription where there is hearing but no case
        var transcriptionByRequester = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(testUser)
            .hearings(List.of(hearing))
            .courtCases(List.of())
            .createdById(testUser.getId())
            .lastModifiedById(testUser.getId())
            .build().getEntity();
        dartsPersistence.save(transcriptionByRequester);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), REQUESTED, transcriptionByRequester);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionByRequester);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());
        requestBuilder.content("");

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionByRequester.getId().intValue())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-19T13:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date", is("2025-03-19")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].urgency").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order").doesNotExist())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscripts_ShouldNotReturnHiddenTranscriptionRequests_WhenNoLinkedHearing() throws Exception {
        // creates a transcription for a different user that should not be returned
        var transcriptionForOtherUser = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(systemUser)
            .createdById(systemUser.getId())
            .lastModifiedById(systemUser.getId())
            .build().getEntity();
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), REQUESTED, transcriptionForOtherUser);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-19T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionForOtherUser);

        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().minimalTranscription();
        hiddenTranscription.setHideRequestFromRequestor(true);

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, hiddenTranscription);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, hiddenTranscription);

        var transcriptionByRequester = PersistableFactory.getTranscriptionTestData().minimalTranscription();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-24T09:00:00Z"), REQUESTED, transcriptionByRequester);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-24T09:00:00Z"), AWAITING_AUTHORISATION, transcriptionByRequester);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());
        requestBuilder.content("");

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionByRequester.getId().intValue())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-24T09:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Sentencing remarks")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].urgency").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order").doesNotExist())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
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
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId().intValue())))
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