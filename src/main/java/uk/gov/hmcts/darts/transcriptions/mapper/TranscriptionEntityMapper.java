package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptions;

@UtilityClass
public class TranscriptionEntityMapper {

    public TranscriptionEntity mapTransactionToTransactionEntity(
        TranscriptionEntity entity, UpdateTranscriptions transaction) {

        if (transaction.getHideRequestFromRequestor() != null) {
            entity.setHideRequestFromRequestor(transaction.getHideRequestFromRequestor());
        }
        return entity;
    }
}
