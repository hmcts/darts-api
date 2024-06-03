package uk.gov.hmcts.darts.transcriptions.auditing;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.AMEND_TRANSCRIPTION_WORKFLOW;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionTestData.minimalTranscription;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionWorkflowTestData.workflowForTranscriptionWithStatus;
import static uk.gov.hmcts.darts.transcriptions.auditing.TranscriptionUpdateAuditActivityProvider.auditActivitiesFor;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

class TranscriptionUpdateAuditActivityProviderTest {

    @Test
    void detectsForwardTransitionOfWorkflowStatus() {
        var transcription = minimalTranscription();
        workflowForTranscriptionWithStatus(transcription, REQUESTED);
        var updateTranscription = new UpdateTranscriptionRequest().transcriptionStatusId(APPROVED.getId());

        var auditActivityProvider = auditActivitiesFor(transcription, updateTranscription);

        assertThat(auditActivityProvider.getAuditActivities()).containsExactly(AMEND_TRANSCRIPTION_WORKFLOW);
    }

    @Test
    void detectsBackwardTransitionOfWorkflowStatus() {
        var transcription = minimalTranscription();
        workflowForTranscriptionWithStatus(transcription, APPROVED);
        var updateTranscription = new UpdateTranscriptionRequest().transcriptionStatusId(REQUESTED.getId());

        var auditActivityProvider = auditActivitiesFor(transcription, updateTranscription);

        assertThat(auditActivityProvider.getAuditActivities()).containsExactly(AMEND_TRANSCRIPTION_WORKFLOW);
    }

    @Test
    void doesntDetectTransitionWhenUpdatingToSameStatus() {
        var transcription = minimalTranscription();
        workflowForTranscriptionWithStatus(transcription, REQUESTED);
        var updateTranscription = new UpdateTranscriptionRequest().transcriptionStatusId(REQUESTED.getId());

        var auditActivityProvider = auditActivitiesFor(transcription, updateTranscription);

        assertThat(auditActivityProvider.getAuditActivities()).isEmpty();
    }
}