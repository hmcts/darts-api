package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsUpdateValidator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;

@Component
public class TranscriptionsUpdateSetHidingValidator implements TranscriptionsUpdateValidator {

    public boolean validate(Optional<TranscriptionEntity> entity, UpdateTranscriptionsItem updateTranscriptionsItem) {
        return entity.filter(transcriptionEntity -> canChangeHiding(transcriptionEntity, updateTranscriptionsItem)).isPresent();
    }

    private boolean canChangeHiding(TranscriptionEntity entity, UpdateTranscriptionsItem updateTranscriptionsItem) {
        boolean verify;
        if (updateTranscriptionsItem.getHideRequestFromRequestor() != null && updateTranscriptionsItem.getHideRequestFromRequestor()) {
            boolean transStateVerify = validateStateForHide(entity.getTranscriptionStatus());
            boolean wfStateVerify = validateWfStateForHide(entity.getTranscriptionWorkflowEntities());
            return transStateVerify || wfStateVerify;
        } else {
            verify = true;
        }
        return verify;
    }

    private boolean validateStateForHide(TranscriptionStatusEntity entity) {
        return entity != null && (Objects.equals(entity.getId(), COMPLETE.getId())
              || Objects.equals(entity.getId(), REJECTED.getId()));
    }

    private boolean validateWfStateForHide(List<TranscriptionWorkflowEntity> entity) {
        return entity.stream().anyMatch(workflow -> validateStateForHide(
              workflow.getTranscriptionStatus()));
    }
}
