package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface EventsService {

    void notifyEvent(DartsEvent dartsEvent);

}
