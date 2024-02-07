package uk.gov.hmcts.darts.log.api;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Service
public interface LogApi {
    void eventReceived(DartsEvent event);
}
