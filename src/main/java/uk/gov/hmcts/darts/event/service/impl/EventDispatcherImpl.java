package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.List;

import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NOT_FOUND;
import static uk.gov.hmcts.darts.event.exception.EventError.TOO_MANY_EVENT_HANDLER;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {

    public static final String NO_EVENT_HANDLER_LOG_MESSAGE_FORMAT = "No event handler could be found for message: %s type: %s and subtype: %s";

    public static final String TOO_MANY_EVENT_HANDLER_MESSAGE_FORMAT = "More than one event handler found for message: %s type: %s and subtype: %s";

    private final List<EventHandler> eventHandlers;

    @Override
    public void receive(DartsEvent event) {
        List<EventHandler> handlers = eventHandlers.stream()
            .filter(handler -> handler.isHandlerFor(event)).toList();

        if (handlers.isEmpty()) {
            throw new DartsApiException(
                EVENT_HANDLER_NOT_FOUND,
                String.format(
                    NO_EVENT_HANDLER_LOG_MESSAGE_FORMAT,
                    event.getMessageId(),
                    event.getType(),
                    event.getSubType()
                )
            );
        } else if (handlers.size() == 1) {
            handlers.get(0).handle(event);
        } else {
            throw new DartsApiException(
                TOO_MANY_EVENT_HANDLER,
                String.format(
                    TOO_MANY_EVENT_HANDLER_MESSAGE_FORMAT,
                    event.getMessageId(),
                    event.getType(),
                    event.getSubType()
                )
            );
        }

    }
}
