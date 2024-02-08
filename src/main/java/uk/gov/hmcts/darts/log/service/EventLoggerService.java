package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface EventLoggerService {
    void eventReceived(DartsEvent event);
}
