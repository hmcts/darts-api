package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.events.model.DartsEvent;

public interface EventHandler {
    void handle(DartsEvent dartsEvent);

    boolean isHandlerFor(DartsEvent event);

}
