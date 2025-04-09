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
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

class TranscriberTranscriptsQueryImplTest extends IntegrationBase {

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @Autowired
    private TranscriberTranscriptsQueryImpl transcriberTranscriptsQuery;

    private UserAccountEntity userAccountEntity;

    private CourthouseEntity courthouse;

    private CourtCaseEntity courtCaseEntity;

    private HearingEntity hearingEntity;

    private static final OffsetDateTime NOW = now(UTC);

    @BeforeEach
    void setUp() {
        courthouse = dartsDatabase.getCourthouseStub().createMinimalCourthouse();
        userAccountEntity = dartsDatabase.getUserAccountStub().createTranscriptionCompanyUser(courthouse);

        hearingEntity = dartsDatabase.getHearingStub().createMinimalHearing();
        courtCaseEntity = hearingEntity.getCourtCase();
    }

    @Test
    void getTranscriptRequestsWithMultipleWorkflows() {
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW.minusDays(2), false
        );

        var approved = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(1), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities().add(approved);
        transcriptionEntity.setTranscriptionStatus(approved.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequestsWithMultipleWorkflowsWithSameWorkflowTimestamp() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveApprovedTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, yesterday, false
        );
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions() {
        TranscriptionEntity transcriptionWithoutDocument = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW, false
        );
        TranscriptionEntity transcriptionWithDocument = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW, false
        );
        // should not be returned
        TranscriptionEntity transcriptionWithHiddenDocument = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW, true
        );

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(2);
        assertThat(transcriberTranscriptions.stream().filter(t -> t.getTranscriptionId().equals(transcriptionWithoutDocument.getId()))).isNotNull();
        assertThat(transcriberTranscriptions.stream().filter(t -> t.getTranscriptionId().equals(transcriptionWithDocument.getId()))).isNotNull();
        assertThat(transcriberTranscriptions
                       .stream()
                       .filter(t -> t.getTranscriptionId().equals(transcriptionWithHiddenDocument.getId()))
                       .findFirst()
        ).isEmpty();
    }

    @Test
    void getTranscriberTranscriptionsWithMultipleWithTranscriberWorkflows() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveWithTranscriberTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW.minusDays(3), false
        );
        var backToApproved = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(2), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        var withTranscriber = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, yesterday, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(backToApproved, withTranscriber));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getStateChangeTs().format(format)).isEqualTo(yesterday.format(format));
    }

    @Test
    void getTranscriberTranscriptionsWithMultipleWorkflowsWithSameWorkflowTimestamp() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveApprovedTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, yesterday, false
        );
        var withTranscriber = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, yesterday, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities().add(withTranscriber);
        transcriptionEntity.setTranscriptionStatus(withTranscriber.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptionsWithMultipleWithTranscriberWorkflowsWithSameWorkflowTimestamp() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveWithTranscriberTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, yesterday, false
        );
        var withTranscriber = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, yesterday, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities().add(withTranscriber);
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getApprovedTranscriptionsCountForCourthouses() {
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveApprovedTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW.minusDays(5), false
        );
        var withTranscriber = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(3), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );
        var backToApproved = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(1), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(withTranscriber, backToApproved));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        Integer approvedTranscriptionCount = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(
            List.of(courthouse.getId()),
            APPROVED.getId(),
            userAccountEntity.getId()
        );

        assertThat(approvedTranscriptionCount).isEqualTo(1);
    }

    @Test
    void getWithTranscriberTranscriptionsCountForCourthouses() {
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveWithTranscriberTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW.minusDays(3), false
        );
        var backToApproved = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(2), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );
        var withTranscriber = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(1), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(backToApproved, withTranscriber));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        Integer approvedTranscriptionCount = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(
            List.of(courthouse.getId()),
            WITH_TRANSCRIBER.getId(),
            userAccountEntity.getId()
        );

        assertThat(approvedTranscriptionCount).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionWithMultipleTranscriberWorkflows_WhenWorkflowReversed() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var reRunRequestedDate = OffsetDateTime.parse("2025-03-20T13:00:00Z");
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        // Create a transcription with multiple workflows REQUESTED -> AWAITING_AUTHORISATION
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, transcriptionDate, false
        );
        // Rewind back to REQUESTED
        var requestedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, reRunRequestedDate, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED)
        );
        // Move workflow to AWAITING_AUTHORISATION
        var awaitingAuthWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, reRunRequestedDate.plusSeconds(1),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION)
        );
        // Move workflow to APPROVED
        var approvedWorkflow = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, approvedDate, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED)
        );

        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(requestedWorkflow, awaitingAuthWorkflow, approvedWorkflow));
        transcriptionEntity.setTranscriptionStatus(approvedWorkflow.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getRequestedTs().format(format)).isEqualTo(transcriptionDate.format(format));
    }
}