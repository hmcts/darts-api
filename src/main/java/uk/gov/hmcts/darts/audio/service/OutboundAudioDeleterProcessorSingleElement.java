package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface OutboundAudioDeleterProcessorSingleElement {

    List<TransientObjectDirectoryEntity> markForDeletion(UserAccountEntity userAccount, TransformedMediaEntity transformedMedia);

    void markMediaRequestAsExpired(MediaRequestEntity mediaRequest, UserAccountEntity userAccount);
}
