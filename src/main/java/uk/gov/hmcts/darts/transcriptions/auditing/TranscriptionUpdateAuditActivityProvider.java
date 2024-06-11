package uk.gov.hmcts.darts.transcriptions.auditing;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditActivityProvider;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.AMEND_TRANSCRIPTION_WORKFLOW;

public class TranscriptionUpdateAuditActivityProvider implements AuditActivityProvider {

    public static TranscriptionUpdateAuditActivityProvider auditActivitiesFor(TranscriptionEntity entity, UpdateTranscriptionRequest patch) {
        return new TranscriptionUpdateAuditActivityProvider(entity, patch);
    }

    private final Set<AuditActivity> auditActivities = new HashSet<>();

    private TranscriptionUpdateAuditActivityProvider() {
    }

    private TranscriptionUpdateAuditActivityProvider(TranscriptionEntity entity, UpdateTranscriptionRequest patch) {
        if (isWorkflowStatusTransitioning(entity, patch)) {
            auditActivities.add(AMEND_TRANSCRIPTION_WORKFLOW);
        }
    }

    @Override
    public Set<AuditActivity> getAuditActivities() {
        return auditActivities;
    }

    private boolean isWorkflowStatusTransitioning(TranscriptionEntity entity, UpdateTranscriptionRequest patch) {
        var latestWorkflow = entity.getLatestTranscriptionWorkflow();

        return latestWorkflow.isEmpty()
            || !Objects.equals(latestWorkflow.get().getTranscriptionStatus().getId(), patch.getTranscriptionStatusId());
    }
}
