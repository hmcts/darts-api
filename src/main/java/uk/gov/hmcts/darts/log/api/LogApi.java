package uk.gov.hmcts.darts.log.api;

import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface LogApi {
    void eventReceived(DartsEvent event);

    void missingCourthouse(DartsEvent event);

    void missingNodeRegistry(DartsEvent event);
}
