package uk.gov.hmcts.darts.transcriptions.helper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;

@UtilityClass
public class UpdateTranscriptionEntityHelper {

    public TranscriptionEntity updateTranscriptionEntity(
          TranscriptionEntity entity, UpdateTranscriptionsItem updateTranscriptionsItem) {

        if (updateTranscriptionsItem.getHideRequestFromRequestor() != null) {
            entity.setHideRequestFromRequestor(updateTranscriptionsItem.getHideRequestFromRequestor());
        }
        return entity;
    }
}
