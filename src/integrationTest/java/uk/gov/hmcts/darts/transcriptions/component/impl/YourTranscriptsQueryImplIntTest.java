package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.transcriptions.model.YourTranscriptsSummary;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

class YourTranscriptsQueryImplIntTest extends IntegrationBase {

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @Autowired
    private YourTranscriptsQueryImpl yourTranscriptsQuery;

    private UserAccountEntity transcriberUser;
    private UserAccountEntity requesterUser;
    private UserAccountEntity approverUser;

    private CourtCaseEntity courtCaseEntity;

    private HearingEntity hearingEntity;

    @BeforeEach
    void setUp() {
        CourthouseEntity courthouse = dartsDatabase.getCourthouseStub().createMinimalCourthouse();
        transcriberUser = dartsDatabase.getUserAccountStub().createTranscriptionCompanyUser(courthouse);
        requesterUser = dartsDatabase.getUserAccountStub().createAuthorisedIntegrationTestUser(courthouse);
        approverUser = dartsDatabase.getUserAccountStub().createJudgeUser(courthouse);
        hearingEntity = dartsDatabase.getHearingStub().createMinimalHearing();
        courtCaseEntity = hearingEntity.getCourtCase();
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionSummaryWithMultipleTranscriberWorkflows_WhenWorkflowReversed() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var reRunRequestedDate = OffsetDateTime.parse("2025-03-20T13:00:00Z");
        // Create a transcription with multiple workflows REQUESTED -> AWAITING_AUTHORISATION
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            requesterUser, courtCaseEntity, hearingEntity, transcriptionDate, false
        );
        // Rewind back to REQUESTED
        var requestedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, reRunRequestedDate, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED)
        );
        requestedWorkflow.setWorkflowActor(requesterUser);
        transcriptionEntity.setTranscriptionStatus(requestedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        // Move workflow to AWAITING_AUTHORISATION
        var awaitingAuthWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, reRunRequestedDate.plusSeconds(1),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION)
        );
        awaitingAuthWorkflow.setWorkflowActor(transcriberUser);
        transcriptionEntity.setTranscriptionStatus(awaitingAuthWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");

        // Move workflow to APPROVED
        var approvedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, approvedDate, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        approvedWorkflow.setWorkflowActor(approverUser);

        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(requestedWorkflow, awaitingAuthWorkflow, approvedWorkflow));
        transcriptionEntity.setTranscriptionStatus(approvedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(yourTranscriptsSummaries.getFirst().getRequestedTs().format(format)).isEqualTo(transcriptionDate.format(format));
    }

    @Test
    void getTranscriptRequests_ReturnsEmptyList_WhenNoTranscriptionsMatchCriteria() {
        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);

        assertThat(yourTranscriptsSummaries).isEmpty();
    }

    @Test
    void getTranscriptRequests_ExcludesHiddenTranscriptions_WhenIncludeHiddenIsFalse() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        TranscriptionEntity hiddenTranscription = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            requesterUser, courtCaseEntity, hearingEntity, transcriptionDate, true
        );
        hiddenTranscription.setHideRequestFromRequestor(true);
        var approvedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            hiddenTranscription, transcriptionDate.plusHours(1), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        approvedWorkflow.setWorkflowActor(approverUser);
        hiddenTranscription.setTranscriptionStatus(approvedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(hiddenTranscription);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);

        assertThat(yourTranscriptsSummaries).isEmpty();
    }

    @Test
    void getTranscriptRequests_ReturnsHiddenTranscriptions_WhenIncludeHiddenIsTrue() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        TranscriptionEntity hiddenTranscription = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            requesterUser, courtCaseEntity, hearingEntity, transcriptionDate, true
        );
        hiddenTranscription.setHideRequestFromRequestor(true);
        var approvedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            hiddenTranscription, transcriptionDate.plusHours(1), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        approvedWorkflow.setWorkflowActor(approverUser);
        hiddenTranscription.setTranscriptionStatus(approvedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(hiddenTranscription);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), true);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionsWithMultipleActors() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a transcription with multiple actors
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            requesterUser, courtCaseEntity, hearingEntity, transcriptionDate, false
        );
        var requestedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, transcriptionDate.plusHours(1), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED)
        );
        requestedWorkflow.setWorkflowActor(requesterUser);

        var approvedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, transcriptionDate.plusHours(2), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        approvedWorkflow.setWorkflowActor(approverUser);

        transcriptionEntity.getTranscriptionWorkflowEntities().addAll(List.of(requestedWorkflow, approvedWorkflow));
        transcriptionEntity.setTranscriptionStatus(approvedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
        assertThat(yourTranscriptsSummaries.getFirst().getStatus()).isEqualTo("Approved");
    }

}
