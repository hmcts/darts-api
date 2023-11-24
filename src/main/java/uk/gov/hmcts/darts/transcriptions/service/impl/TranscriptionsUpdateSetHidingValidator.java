package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptions;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsUpdateValidator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;

@Component
public class TranscriptionsUpdateSetHidingValidator implements TranscriptionsUpdateValidator {

    public boolean validate(Optional<TranscriptionEntity> entity, UpdateTranscriptions transaction) {
        return entity.filter(transcriptionEntity -> canChangeHiding(transcriptionEntity, transaction)).isPresent();
    }

    private boolean isHidingSet(UpdateTranscriptions transcription) {
        return transcription.getHideRequestFromRequestor() != null;
    }

    private boolean canChangeHiding(TranscriptionEntity entity, UpdateTranscriptions transaction) {
        boolean verify;
        if (transaction.getHideRequestFromRequestor() != null && transaction.getHideRequestFromRequestor()) {
            boolean transStateverify = validateStateForHide(entity.getTranscriptionStatus());
            boolean wfStateVerify = validateWfStateForHide(entity.getTranscriptionWorkflowEntities());
            return transStateverify || wfStateVerify;
        } else {
            verify = true;
        }
        return verify;
    }

    private boolean validateStateForHide(TranscriptionStatusEntity entity) {
        return entity != null && Objects.equals(entity.getId(), COMPLETE.getId())
            || Objects.equals(entity.getId(), REJECTED.getId());
    }

    private boolean validateWfStateForHide(List<TranscriptionWorkflowEntity> entity) {
        return entity.stream().anyMatch(workflow -> validateStateForHide(
                workflow.getTranscriptionStatus()));
    }
}
