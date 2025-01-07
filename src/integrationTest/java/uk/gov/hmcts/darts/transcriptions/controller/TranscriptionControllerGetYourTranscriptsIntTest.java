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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.net.URI;
import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    public static final int RETENTION_CONFIDENCE_SCORE = 2;
    public static final String RETENTION_CONFIDENCE_REASON = "RetentionConfidenceReason";
    private static final String REQUESTED_TRANSCRIPTION_COMMENT = "Requested transcription";

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
            .createAndSaveAwaitingAuthorisationTranscription(authorisationStub.getTestUser(), courtCase, hearing, YESTERDAY, false);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );
        requestBuilder.content("");
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions", hasSize(2)))
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[1].case_id", is(courtCase.getId())))
            .andExpect(jsonPath(
                "$.requester_transcriptions[1].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is(transcription.getCourtHouse().get().getDisplayName())))

            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].urgency").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.transcription_urgency_id").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_urgency.description").doesNotExist())
            .andExpect(jsonPath("$.requester_transcriptions[0]." +
                                    "transcription_urgency.priority_order").doesNotExist())

            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())

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
    void getYourTranscriptsApproverOver90DaysShouldNotReturn() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        dartsDatabase.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(
                systemUser,
                courtCase,
                authorisationStub.getHearingEntity(), MINUS_90_DAYS
            );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
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
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(nonHiddenTranscription.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[1].transcription_id", is(transcriptionEntity.getId())))
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
}