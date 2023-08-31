package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.event.service.EventHandler;
import uk.gov.hmcts.darts.events.model.DartsEvent;

import java.util.List;

import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {

    public static final String LOG_MESSAGE_FORMAT = "No event handler could be found for message: %s type: %s and subtype: %s";

    private final List<EventHandler> eventHandlers;

    @Override
    public void receive(DartsEvent event) {
        eventHandlers.stream()
            .filter(handler -> handler.isHandlerFor(event))
            .findFirst().orElseThrow(() -> new DartsApiException(
                EVENT_HANDLER_NOT_FOUND,
                String.format(LOG_MESSAGE_FORMAT, event.getMessageId(), event.getType(), event.getSubType())
            ))
            .handle(event);
    }
}
