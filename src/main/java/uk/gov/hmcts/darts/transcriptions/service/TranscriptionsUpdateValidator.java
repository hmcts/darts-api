package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;

import java.util.Optional;

@FunctionalInterface
public interface TranscriptionsUpdateValidator {
    boolean validate(Optional<TranscriptionEntity> entity, UpdateTranscriptionsItem updateTranscriptionsItem);
}
