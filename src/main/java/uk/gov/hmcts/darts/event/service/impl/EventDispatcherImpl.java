package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NOT_FOUND_IN_DB;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {

    public static final String NO_HANDLER_IN_DB_MESSAGE = "No event handler could be found in the database for messageId: %s type: %s and subtype: %s.";
    public static final String HANDLER_NOT_FOUND_MESSAGE = "No event handler could be found for messageId: %s type: %s and subtype: %s, just returning OK";

    private final List<EventHandler> eventHandlers;
    private final EventHandlerRepository eventHandlerRepository;
    private final Map<String, EventHandlerEntity> eventHandlerCache = new ConcurrentHashMap<>();

    private static void logEvent(DartsEvent event, EventHandler foundHandler) {
        var caseNumbers = String.join(", ", ofNullable(event.getCaseNumbers()).orElse(emptyList()));
        log.info(
              "Executing event handler: {} for event: {} and case number(s): {}",
              foundHandler.getClass().getName(),
              requireNonNullElse(event.getEventId(), "non provided"),
              caseNumbers);
    }

    @Override
    public void receive(DartsEvent event) {
        EventHandlerEntity foundHandlerEntity = findHandler(event);
        Optional<EventHandler> foundHandler = eventHandlers.stream()
              .filter(handler -> handler.isHandlerFor(foundHandlerEntity.getHandler()))
              .findAny();
        if (foundHandler.isPresent()) {
            logEvent(event, foundHandler.get());
            foundHandler.get().handle(event, foundHandlerEntity);
        } else {
            // Event registered in DB, but no handler defined...just log and return OK.
            log.warn(format(HANDLER_NOT_FOUND_MESSAGE, event.getMessageId(), event.getType(), event.getSubType()));
        }
    }

    private EventHandlerEntity findHandler(DartsEvent event) {
        String key = event.getType() + "___" + event.getSubType();
        EventHandlerEntity cachedVersion = eventHandlerCache.get(key);
        if (cachedVersion != null) {
            log.trace("cache hit for key {}", key);
            return cachedVersion;
        }
        log.trace("cache miss for key {}", key);

        List<EventHandlerEntity> foundMappings = eventHandlerRepository.findByTypeAndSubType(
              event.getType(),
              event.getSubType()
        );
        if (foundMappings.isEmpty()) {
            log.warn(format(HANDLER_NOT_FOUND_MESSAGE, event.getMessageId(), event.getType(), event.getSubType()));
            throw new DartsApiException(
                  EVENT_HANDLER_NOT_FOUND_IN_DB,
                  format(NO_HANDLER_IN_DB_MESSAGE, event.getMessageId(), event.getType(), event.getSubType())
            );
        }

        //first entry will be what we want because of ordering in sql will put the default null entry last.
        EventHandlerEntity eventHandlerEntity = foundMappings.get(0);
        eventHandlerCache.put(key, eventHandlerEntity);
        return eventHandlerEntity;
    }

}
