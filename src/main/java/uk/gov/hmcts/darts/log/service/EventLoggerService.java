package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface EventLoggerService {
    void eventReceived(DartsEvent event);

    void missingCourthouse(DartsEvent event);

    void missingNodeRegistry(DartsEvent event);

    void manualObfuscation(EventEntity eventEntity);
}