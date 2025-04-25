package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
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
 * Integration test for the TranscriptionController class, specifically for the "Get Your Transcripts" endpoint for modernised data.
 */
@AutoConfigureMockMvc
class TranscriptionControllerGetYourTranscriptsIntTest extends IntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions");

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private MockMvc mockMvc;

    private TranscriptionEntity transcriptionEntity;
    private UserAccountEntity testUser;
    private UserAccountEntity systemUser;

    private static final OffsetDateTime YESTERDAY = now(UTC).minusDays(1).withHour(9).withMinute(0)
        .withSecond(0).withNano(0);

    private static final OffsetDateTime MINUS_90_DAYS = now(UTC).minusDays(90);

    @MockitoBean
    private CurrentTimeHelper currentTimeHelper;

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @BeforeEach
    void beforeEach() {
        openInViewUtil.openEntityManager();
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();

        systemUser = authorisationStub.getSystemUser();
        testUser = authorisationStub.getTestUser();

    }

    @Test
    void getYourTranscriptsShouldReturnRequesterOnlyOk() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        var hearing = authorisationStub.getHearingEntity();
        TranscriptionEntity transcription = transcriptionStub
            .createAndSaveCompletedTranscription(authorisationStub.getTestUser(), courtCase, hearing, YESTERDAY, true);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        TranscriptionUrgencyEntity urgencyEntity = transcriptionStub.getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcription.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id", is(TranscriptionUrgencyEnum.STANDARD.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description", is(urgencyEntity.getDescription())))
            .andExpect(jsonPath("$.requester_transcriptions[0]." +
                                    "transcription_urgency.priority_order", is(urgencyEntity.getPriorityOrder())))

            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsShouldReturnRequesterOnlyOkWithNoUrgency() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        var hearing = authorisationStub.getHearingEntity();
        TranscriptionEntity transcription = transcriptionStub
            .createAndSaveAwaitingAuthorisationTranscription(authorisationStub.getTestUser(), courtCase, hearing, YESTERDAY.minusMinutes(1), false);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );
        requestBuilder.content("");
        //Ensures sorting is correct
        assertThat(getRequestedTs(transcriptionEntity)).isAfter(getRequestedTs(transcription));
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(2)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is(getRequestedTsStr(transcriptionEntity))))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number", is(courtCase.getCaseNumber())))


            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_id", is(transcription.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[1].requested_ts", is(getRequestedTsStr(transcription))))
            .andExpect(jsonPath("$.requester_transcriptions[1].courthouse_name", is(transcription.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[1].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[1].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[1].urgency").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[1]." + "transcription_urgency.priority_order").doesNotExist())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                systemUser.getId()
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions").isEmpty())
            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsShouldReturnRequesterAndApproverCombinedOk() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        var systemUserTranscription = dartsDatabase.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(
                systemUser,
                courtCase,
                authorisationStub.getHearingEntity(), now(UTC)
            );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        TranscriptionUrgencyEntity urgencyEntity = transcriptionStub.getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_id",
                is(courtCase.getId())
            ))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcriptionEntity.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id", is(TranscriptionUrgencyEnum.STANDARD.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description", is(urgencyEntity.getDescription())))
            .andExpect(jsonPath("$.requester_transcriptions[0]." +
                                    "transcription_urgency.priority_order", is(urgencyEntity.getPriorityOrder())))

            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())

            .andExpect(jsonPath("$.approver_transcriptions[0].transcription_id", is(systemUserTranscription.getId())))
            .andExpect(jsonPath(
                "$.approver_transcriptions[0].case_id",
                is(courtCase.getId())
            ));
    }

    @Test
    void getYourTranscriptsRequesterShouldNotReturnHidden() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        var hearing = authorisationStub.getHearingEntity();
        // create a second transcription with a non-hidden document - should be returned
        TranscriptionEntity nonHiddenTranscription = transcriptionStub.createAndSaveCompletedTranscriptionWithDocument(
            authorisationStub.getTestUser(), courtCase, hearing, YESTERDAY, false);
        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(nonHiddenTranscription, systemUser, true);

        // and one with a hidden document - should not be returned
        TranscriptionEntity hidden = transcriptionStub
            .createAndSaveCompletedTranscriptionWithDocument(authorisationStub.getTestUser(), courtCase, hearing, YESTERDAY, true);
        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(hidden, systemUser, true);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI).header("user_id", testUser.getId());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(2)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is(getRequestedTsStr(transcriptionEntity))))
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_id", is(nonHiddenTranscription.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[1].requested_ts", is(getRequestedTsStr(nonHiddenTranscription))))
            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsApproverShouldNotReturnHidden() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        TranscriptionEntity systemUserTranscription = dartsDatabase.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(
                systemUser,
                courtCase,
                authorisationStub.getHearingEntity(), now(UTC)
            );
        // an "approver" transcription that has a hidden document should not be returned
        TranscriptionEntity systemUserTranscriptionWithHiddenDoc = dartsDatabase.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(
                systemUser,
                courtCase,
                authorisationStub.getHearingEntity(), now(UTC)
            );
        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(systemUserTranscriptionWithHiddenDoc, systemUser, true);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI).header("user_id", testUser.getId());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approver_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.approver_transcriptions[0].transcription_id", is(systemUserTranscription.getId())));
    }

    @Test
    void getYourTranscriptsShouldNotReturnTranscriptWhenIsCurrentFalse() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        transcriptionStub.createAndSaveAwaitingAuthorisationTranscription(
            systemUser,
            courtCase,
            authorisationStub.getHearingEntity(), now(UTC), false, false
        );

        transcriptionStub.createAndSaveAwaitingAuthorisationTranscription(
            authorisationStub.getTestUser(),
            courtCase,
            authorisationStub.getHearingEntity(), YESTERDAY, false, false
        );


        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsWithApprovedStatusShouldReturnApproveOnTimeStamp() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        OffsetDateTime now = now();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();
        createTranscriptionWorkflow(systemUser, now, APPROVED, transcriptionEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        TranscriptionUrgencyEntity urgencyEntity = transcriptionStub.getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_id",
                is(courtCase.getId())
            ))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcriptionEntity.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Approved")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id", is(TranscriptionUrgencyEnum.STANDARD.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description", is(urgencyEntity.getDescription())))
            .andExpect(jsonPath("$.requester_transcriptions[0]." +
                                    "transcription_urgency.priority_order", is(urgencyEntity.getPriorityOrder())))

            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].approved_ts").isString());

    }

    @Test
    void getYourTranscripts_ShouldReturnSingleWorkflow_WhenWorkflowHasBeenRevertedToRequested() throws Exception {
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionEntity);

        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T14:00:00Z"), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T14:00:00Z"), AWAITING_AUTHORISATION, transcriptionEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());

        TranscriptionUrgencyEntity urgencyEntity = transcriptionStub.getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD);
        var courtCase = authorisationStub.getCourtCaseEntity();

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number", is(courtCase.getCaseNumber())))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcriptionEntity.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id",
                                is(TranscriptionUrgencyEnum.STANDARD.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description", is(urgencyEntity.getDescription())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order", is(urgencyEntity.getPriorityOrder())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-20T13:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].approved_ts").doesNotExist());

    }

    @Test
    void getYourTranscripts_ShouldReturnSingleWorkflow_WhenWorkflowHasBeenRevertedToApproved() throws Exception {
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(testUser, OffsetDateTime.parse("2025-03-20T13:00:00Z"), AWAITING_AUTHORISATION, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T14:00:00Z"), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T15:00:00Z"), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(systemUser, OffsetDateTime.parse("2025-03-23T17:00:00Z"), APPROVED, transcriptionEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header("user_id", testUser.getId());

        TranscriptionUrgencyEntity urgencyEntity = transcriptionStub.getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD);
        var courtCase = authorisationStub.getCourtCaseEntity();

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(1)))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_number", is(courtCase.getCaseNumber())))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcriptionEntity.getCourtHouse().get().getDisplayName())))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Approved")))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id",
                                is(TranscriptionUrgencyEnum.STANDARD.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description", is(urgencyEntity.getDescription())))
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.priority_order", is(urgencyEntity.getPriorityOrder())))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts", is("2025-03-20T13:00:00Z")))
            .andExpect(jsonPath("$.requester_transcriptions[0].approved_ts", is("2025-03-23T14:00:00Z")));

    }

    private void createTranscriptionWorkflow(UserAccountEntity systemUser, OffsetDateTime now, TranscriptionStatusEnum transcriptionStatusEnum,
                                             TranscriptionEntity transcriptionEntity) {
        TranscriptionWorkflowEntity transcriptionWorkflowEntity =
            transcriptionStub.createTranscriptionWorkflowEntity(authorisationStub.getTranscriptionEntity(), systemUser,
                                                                now,
                                                                transcriptionStub.getTranscriptionStatusByEnum(transcriptionStatusEnum));
        transcriptionEntity.getTranscriptionWorkflowEntities().add(transcriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(transcriptionStub.getTranscriptionStatusByEnum(transcriptionStatusEnum));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);
    }


    private OffsetDateTime getRequestedTs(TranscriptionEntity transcriptionEntity) {
        return transcriptionEntity.getTranscriptionWorkflowEntities()
            .stream()
            .filter(transcriptionWorkflowEntity -> transcriptionWorkflowEntity.getTranscriptionStatus().getId().equals(1))
            .findFirst().get().getWorkflowTimestamp();
    }

    private String getRequestedTsStr(TranscriptionEntity transcriptionEntity) {
        return getRequestedTs(transcriptionEntity).format(DateTimeFormatter.ISO_DATE_TIME);
    }
}