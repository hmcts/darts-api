package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface EventDispatcher {

    void receive(DartsEvent dartsEvent);
}
