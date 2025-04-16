package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.DartsEvent;

@FunctionalInterface
public interface EventDispatcher {
    void receive(DartsEvent dartsEvent);
}
