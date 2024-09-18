package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Disabled("Impacted by V1_364_*.sql")
class TranscriptionWorkflowAuditTest extends IntegrationBase {

    @Autowired
    private TranscriptionService transcriptionService;

    @Autowired
    private GivenBuilder given;

    @Test
    void auditsWhenTranscriptionsAreCreated() {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var transcriptionRequestDetails = createTranscriptionRequestDetailsWithDefaults();

        var createTranscriptionResponse = transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true);

        var transcriptionRevisions = dartsDatabase.findTranscriptionRevisionsFor(createTranscriptionResponse.getTranscriptionId());
        assertThat(transcriptionRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
    }

    @Test
    void auditsWhenTranscriptionsWorkflowsAreTransitioned() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var transcriptionRequestDetails = createTranscriptionRequestDetailsWithDefaults();
        var createTranscriptionResponse = transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, false);

        transcriptionService.updateTranscriptionAdmin(
            createTranscriptionResponse.getTranscriptionId(),
            new UpdateTranscriptionRequest().transcriptionStatusId(7).workflowComment("new comment"),
            true);

        var auditActivity = findAuditActivity("Amend Transcription Workflow", dartsDatabase.findAudits());
        assertThat(auditActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());
        assertThat(auditActivity.getCourtCase().getId()).isEqualTo(transcriptionRequestDetails.getCaseId());

        var transcriptionWorkflowRevisions = dartsDatabase.findTranscriptionWorkflowRevisionsFor(createTranscriptionResponse.getTranscriptionId());
        assertThat(transcriptionWorkflowRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);

        var transcriptionCommentRevisions = dartsDatabase.findTranscriptionCommentRevisionsFor(createTranscriptionResponse.getTranscriptionId());
        assertThat(transcriptionCommentRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
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