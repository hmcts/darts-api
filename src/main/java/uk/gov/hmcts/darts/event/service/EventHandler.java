package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface EventHandler {
    void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler);

    boolean isHandlerFor(String handlerName);

}
