package uk.gov.hmcts.darts.log.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Service
public interface EventLoggerService {
    void eventReceived(DartsEvent event);
}
