package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.List;

@FunctionalInterface
public interface OutboundAudioDeleterProcessor {
    List<TransientObjectDirectoryEntity> markForDeletion(Integer batchSize);
}
