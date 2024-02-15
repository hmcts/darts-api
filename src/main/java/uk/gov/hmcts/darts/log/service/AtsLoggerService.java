package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

public interface AtsLoggerService {
    void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity);
}
