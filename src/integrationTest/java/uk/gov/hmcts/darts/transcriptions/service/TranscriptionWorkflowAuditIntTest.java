package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

class TranscriptionWorkflowAuditIntTest extends IntegrationBase {

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private GivenBuilder given;

    @Test
    void updateTranscriptionAdmin_ShouldAudit_WhenTranscriptionsAreCreated() {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var transcriptionRequestDetails = createTranscriptionRequestDetailsWithDefaults();

        var createTranscriptionResponse = transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true);

        var transcriptionRevisions = dartsDatabase.findTranscriptionRevisionsFor(createTranscriptionResponse.getTranscriptionId());
        assertThat(transcriptionRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
    }

    @Test
    void updateTranscriptionAdmin_ShouldAudit_WhenTranscriptionsWorkflowsAreTransitioned() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var transcriptionRequestDetails = createTranscriptionRequestDetailsWithDefaults();
        var createTranscriptionResponse = transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, false);

        transcriptionService.updateTranscriptionAdmin(
            createTranscriptionResponse.getTranscriptionId(),
            new UpdateTranscriptionRequest().transcriptionStatusId(7).workflowComment("new comment"),
            true);
        transactionalUtil.executeInTransaction(() -> {
            var auditActivity = findAuditActivity("Amend Transcription Workflow", dartsDatabase.findAudits());
            assertThat(auditActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());
            assertThat(auditActivity.getCourtCase().getId()).isEqualTo(transcriptionRequestDetails.getCaseId());

            var transcriptionWorkflowRevisions = dartsDatabase.findTranscriptionWorkflowRevisionsFor(createTranscriptionResponse.getTranscriptionId());
            assertThat(transcriptionWorkflowRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);

            var transcriptionCommentRevisions = dartsDatabase.findTranscriptionCommentRevisionsFor(createTranscriptionResponse.getTranscriptionId());
            assertThat(transcriptionCommentRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
        });
    }

    @Test
    void updateTranscriptionAdmin_Succeeds_WhenManualTranscriptionWorkflowTransitionsFromAwaitingAuthToRequestThenMovedToAwaitingAuth() {
        // Given
        var adminUser = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            var systemUser = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
            HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
                "SOME_CASE_ID",
                "SOME_COURTHOUSE",
                "SOME_COURTROOM",
                LocalDateTime.now()
            );

            var hearing = dartsDatabase.save(hearingEntity);
            var courtroom = hearing.getCourtroom();
            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity awaitingAuthTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(
                AWAITING_AUTHORISATION);
            final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, awaitingAuthTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser, courtroom);

            final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
                .findById(transcription.getId()).orElseThrow();
            assertEquals(AWAITING_AUTHORISATION.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());


            return transcription;
        });
        // When
        transcriptionService.updateTranscriptionAdmin(
            transcriptionEntity.getId(),
            new UpdateTranscriptionRequest().transcriptionStatusId(REQUESTED.getId()).workflowComment("new comment"),
            true);

        // Then
        var updatedTranscription = dartsDatabase.getTranscriptionRepository().findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(updatedTranscription.getTranscriptionStatus().getId(), AWAITING_AUTHORISATION.getId());

        var transcriptionWorkflows = dartsDatabase.getTranscriptionWorkflowRepository()
            .findByTranscriptionOrderByWorkflowTimestampDesc(updatedTranscription);
        assertEquals(2, transcriptionWorkflows.size());

        assertEquals(AWAITING_AUTHORISATION.getId(), transcriptionWorkflows.get(0).getTranscriptionStatus().getId());
        assertEquals(REQUESTED.getId(), transcriptionWorkflows.get(1).getTranscriptionStatus().getId());

        transactionalUtil.executeInTransaction(() -> {
            var auditActivity = findAuditActivity("Amend Transcription Workflow", dartsDatabase.findAudits());
            assertThat(auditActivity.getUser().getId()).isEqualTo(adminUser.getId());

            var transcriptionWorkflowRevisions = dartsDatabase.findTranscriptionWorkflowRevisionsFor(transcriptionEntity.getId());
            assertThat(transcriptionWorkflowRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);

            var transcriptionCommentRevisions = dartsDatabase.findTranscriptionCommentRevisionsFor(transcriptionEntity.getId());
            assertThat(transcriptionCommentRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
        });
    }

    private AuditEntity findAuditActivity(String activity, List<AuditEntity> audits) {
        return audits.stream()
            .filter(audit -> activity.equals(audit.getAuditActivity().getName()))
            .findFirst().orElseThrow();
    }

    private TranscriptionRequestDetails createTranscriptionRequestDetailsWithDefaults() {
        var hearing = dartsDatabase.save(PersistableFactory.getHearingTestData().someMinimalHearing());
        var startTime = OffsetDateTime.now();

        return new TranscriptionRequestDetails()
            .hearingId(hearing.getId())
            .caseId(hearing.getCourtCase().getId())
            .transcriptionUrgencyId(STANDARD.getId())
            .transcriptionTypeId(TranscriptionTypeEnum.ARGUMENT_AND_SUBMISSION_OF_RULING.getId())
            .comment("Some comment")
            .startDateTime(startTime)
            .endDateTime(startTime.plusHours(1));
    }

}