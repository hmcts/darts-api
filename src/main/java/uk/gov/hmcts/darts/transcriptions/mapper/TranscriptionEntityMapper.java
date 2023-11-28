package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;

@UtilityClass
public class TranscriptionEntityMapper {

    public TranscriptionEntity mapTranscriptionToTranscriptionEntity(
        TranscriptionEntity entity, UpdateTranscriptionsItem updateTranscriptionsItem) {

        if (updateTranscriptionsItem.getHideRequestFromRequestor() != null) {
            entity.setHideRequestFromRequestor(updateTranscriptionsItem.getHideRequestFromRequestor());
        }
        return entity;
    }
}
