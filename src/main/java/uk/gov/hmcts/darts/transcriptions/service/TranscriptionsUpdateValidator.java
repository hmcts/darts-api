package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptions;

import java.util.Optional;

public interface TranscriptionsUpdateValidator {
    boolean validate(Optional<TranscriptionEntity> entity, UpdateTranscriptions transaction);
}
