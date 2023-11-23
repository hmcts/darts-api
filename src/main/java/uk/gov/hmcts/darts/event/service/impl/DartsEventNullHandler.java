package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Service
@RequiredArgsConstructor
@Slf4j
/*
 * This class is used for events that should have no action taken against them.
 */
public class DartsEventNullHandler extends EventHandlerBase {
    @Override
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        log.debug(
            "Null handler selected for message: {} type: {} and subtype: {}. ",
            dartsEvent.getMessageId(),
            dartsEvent.getType(),
            dartsEvent.getSubType()
        );
    }
}
