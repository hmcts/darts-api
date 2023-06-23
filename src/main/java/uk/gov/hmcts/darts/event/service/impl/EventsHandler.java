package uk.gov.hmcts.darts.event.service.impl;

import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface EventsHandler {
    void handle(DartsEvent dartsEvent);

    boolean isHandlerFor(String type, String subType);

}
