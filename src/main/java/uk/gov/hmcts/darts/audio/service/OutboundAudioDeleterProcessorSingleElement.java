package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.List;

public interface OutboundAudioDeleterProcessorSingleElement {

    List<TransientObjectDirectoryEntity> markForDeletion(TransformedMediaEntity transformedMedia);

    void markMediaRequestAsExpired(MediaRequestEntity mediaRequest);
}
