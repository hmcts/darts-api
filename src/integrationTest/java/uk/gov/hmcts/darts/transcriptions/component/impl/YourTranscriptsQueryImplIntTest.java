package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.YourTranscriptsSummary;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

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
        approverUser = dartsDatabase.getUserAccountStub().createApproverUser(courthouse);
        hearingEntity = dartsDatabase.getHearingStub().createMinimalHearing();
        courtCaseEntity = hearingEntity.getCourtCase();
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionSummaryWithReversedWorkflowAndWithMultipleTranscriberWorkflows_whenLinkedHearing() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var reRunRequestedDate = OffsetDateTime.parse("2025-03-20T13:00:00Z");
        // Create a transcription with multiple workflows REQUESTED -> AWAITING_AUTHORISATION
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .hearings(List.of(hearingEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();

        createTranscriptionWorkflow(requesterUser, transcriptionDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(transcriberUser, transcriptionDate.plusSeconds(1), AWAITING_AUTHORISATION, transcriptionEntity);
        // Rewind back to REQUESTED
        createTranscriptionWorkflow(requesterUser, reRunRequestedDate, REQUESTED, transcriptionEntity);
        // Move workflow to AWAITING_AUTHORISATION
        createTranscriptionWorkflow(transcriberUser, reRunRequestedDate.plusSeconds(1), AWAITING_AUTHORISATION, transcriptionEntity);
        // Move workflow to APPROVED
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        createTranscriptionWorkflow(approverUser, approvedDate, APPROVED, transcriptionEntity);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);
        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(yourTranscriptsSummaries.getFirst().getRequestedTs().format(format)).isEqualTo(transcriptionDate.format(format));
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionSummaryWithReversedWorkflowAndWithMultipleTranscriberWorkflows_whenLinkedCase() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var reRunRequestedDate = OffsetDateTime.parse("2025-03-20T13:00:00Z");
        // Create a transcription with multiple workflows REQUESTED -> AWAITING_AUTHORISATION
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();

        createTranscriptionWorkflow(requesterUser, transcriptionDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(transcriberUser, transcriptionDate.plusSeconds(1), AWAITING_AUTHORISATION, transcriptionEntity);
        // Rewind back to REQUESTED
        createTranscriptionWorkflow(requesterUser, reRunRequestedDate, REQUESTED, transcriptionEntity);
        // Move workflow to AWAITING_AUTHORISATION
        createTranscriptionWorkflow(transcriberUser, reRunRequestedDate.plusSeconds(1), AWAITING_AUTHORISATION, transcriptionEntity);
        // Move workflow to APPROVED
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        createTranscriptionWorkflow(approverUser, approvedDate, APPROVED, transcriptionEntity);

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
    void getTranscriptRequestsWithIncludeHiddenIsFalse_ExcludesHiddenTranscriptions_whenLinkedHearing() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .hearings(List.of(hearingEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();

        hiddenTranscription.setHideRequestFromRequestor(true);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(1), REQUESTED, hiddenTranscription);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(2), AWAITING_AUTHORISATION, hiddenTranscription);
        createTranscriptionWorkflow(approverUser, transcriptionDate.plusHours(3), APPROVED, hiddenTranscription);
        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);
        assertThat(yourTranscriptsSummaries).isEmpty();
    }

    @Test
    void getTranscriptRequestsWithIncludeHiddenIsFalse_ExcludesHiddenTranscriptions_whenLinkedCase() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();

        hiddenTranscription.setHideRequestFromRequestor(true);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(1), REQUESTED, hiddenTranscription);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(2), AWAITING_AUTHORISATION, hiddenTranscription);
        createTranscriptionWorkflow(approverUser, transcriptionDate.plusHours(3), APPROVED, hiddenTranscription);
        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);
        assertThat(yourTranscriptsSummaries).isEmpty();
    }

    @Test
    void getTranscriptRequestsWithIncludeHiddenIsTrue_IncludesHiddenTranscriptions_whenLinkedHearing() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .hearings(List.of(hearingEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();
        hiddenTranscription.setHideRequestFromRequestor(true);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(1), REQUESTED, hiddenTranscription);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(2), AWAITING_AUTHORISATION, hiddenTranscription);
        createTranscriptionWorkflow(approverUser, transcriptionDate.plusHours(3), APPROVED, hiddenTranscription);
        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), true);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequestsWithIncludeHiddenIsTrue_IncludesHiddenTranscriptions_whenLinkedCase() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();
        hiddenTranscription.setHideRequestFromRequestor(true);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(1), REQUESTED, hiddenTranscription);
        createTranscriptionWorkflow(requesterUser, transcriptionDate.plusHours(2), AWAITING_AUTHORISATION, hiddenTranscription);
        createTranscriptionWorkflow(approverUser, transcriptionDate.plusHours(3), APPROVED, hiddenTranscription);
        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), true);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionsWithMultipleActors_whenLinkedHearing() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a transcription with multiple actors
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .hearings(List.of(hearingEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();

        createTranscriptionWorkflow(requesterUser, transcriptionDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(approverUser, transcriptionDate.plusHours(1), APPROVED, transcriptionEntity);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
        assertThat(yourTranscriptsSummaries.getFirst().getStatus()).isEqualTo("Approved");
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionsWithMultipleActors_whenLinkedCase() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a transcription with multiple actors
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();

        createTranscriptionWorkflow(requesterUser, transcriptionDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(approverUser, transcriptionDate.plusHours(1), APPROVED, transcriptionEntity);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getRequesterTranscriptions(approverUser.getId(), false);

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
        assertThat(yourTranscriptsSummaries.getFirst().getStatus()).isEqualTo("Approved");
    }

    @Disabled("Disabled due to not working as expected")
    @Test
    void getApproverTranscriptions_ReturnsTranscriptionSummaryWithMultipleTranscriberWorkflows_WhenWorkflowReversedToApproved() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        // Create a transcription with multiple workflows
        // AWAITING_AUTH -> REQUESTED -> AWAITING_AUTH -> APPROVED -> WITH_TRANSCRIBER -> APPROVED
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            requesterUser, courtCaseEntity, hearingEntity, transcriptionDate, false
        );
        // Rewind back to REQUESTED
        var requestedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, transcriptionDate.plusSeconds(30), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED)
        );
        requestedWorkflow.setWorkflowActor(requesterUser);
        transcriptionEntity.setTranscriptionStatus(requestedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        // Move workflow to AWAITING_AUTHORISATION
        var awaitingAuthWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, transcriptionDate.plusSeconds(35),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION)
        );
        awaitingAuthWorkflow.setWorkflowActor(requesterUser);
        transcriptionEntity.setTranscriptionStatus(awaitingAuthWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");

        // Move workflow to APPROVED
        var approvedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, approvedDate, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        approvedWorkflow.setWorkflowActor(approverUser);
        transcriptionEntity.setTranscriptionStatus(approvedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        // Move workflow to WITH_TRANSCRIBER
        var withTranscriberWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, approvedDate, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );
        withTranscriberWorkflow.setWorkflowActor(transcriberUser);
        transcriptionEntity.setTranscriptionStatus(withTranscriberWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        var reRunApprovedDate = OffsetDateTime.parse("2025-03-20T17:00:00Z");

        // Rewind back to APPROVED
        var approvedWorkflow2 = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, reRunApprovedDate,
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        approvedWorkflow2.setWorkflowActor(approverUser);
        transcriptionEntity.setTranscriptionStatus(approvedWorkflow2.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        // Rewind back to AWAITING_AUTHORISATION
        var awaitingAuthWorkflow2 = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, reRunApprovedDate.plusSeconds(1),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION)
        );
        approvedWorkflow2.setWorkflowActor(approverUser);
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(requestedWorkflow, awaitingAuthWorkflow, approvedWorkflow, withTranscriberWorkflow, approvedWorkflow2, awaitingAuthWorkflow2));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriptionEntity> allTranscriptions = dartsDatabase.getTranscriptionRepository().findAll();
        assertThat(allTranscriptions).isNotEmpty();

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getApproverTranscriptions(approverUser.getId());

        assertThat(yourTranscriptsSummaries.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(yourTranscriptsSummaries.getFirst().getApprovedTs().format(format)).isEqualTo(approvedDate.format(format));
    }

    @Test
    void getApproverTranscriptions_ExcludesHiddenTranscriptions_whenLinkedHearing() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .hearings(List.of(hearingEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();
        hiddenTranscription.setHideRequestFromRequestor(true);
        createTranscriptionWorkflow(approverUser, transcriptionDate, APPROVED, hiddenTranscription);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getApproverTranscriptions(approverUser.getId());
        assertThat(yourTranscriptsSummaries).isEmpty();
    }

    @Test
    void getApproverTranscriptions_ExcludesHiddenTranscriptions_whenLinkedCase() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");

        // Create a hidden transcription
        var hiddenTranscription = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(requesterUser)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(transcriptionDate)
            .build().getEntity();
        hiddenTranscription.setHideRequestFromRequestor(true);
        createTranscriptionWorkflow(approverUser, transcriptionDate, APPROVED, hiddenTranscription);

        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getApproverTranscriptions(approverUser.getId());
        assertThat(yourTranscriptsSummaries).isEmpty();
    }

    @Test
    void getApproverTranscriptions_ReturnsEmptyList_WhenNoTranscriptionsMatchCriteria() {
        List<YourTranscriptsSummary> yourTranscriptsSummaries = yourTranscriptsQuery.getApproverTranscriptions(approverUser.getId());

        assertThat(yourTranscriptsSummaries).isEmpty();
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
