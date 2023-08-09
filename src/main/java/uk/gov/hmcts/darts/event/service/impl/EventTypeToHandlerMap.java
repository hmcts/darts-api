package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EventTypeToHandlerMap {

    private final Map<HandlerKey, Pair<Integer, String>> eventTypeToIdAndName = new ConcurrentHashMap<>();
    private final Map<HandlerKey, String> eventTypeToHandler = new ConcurrentHashMap<>();
    private final EventHandlerRepository eventHandlerRepository;

    public boolean hasMapping(DartsEvent event, String simpleName) {
        var key = buildKey(event);
        if (Objects.equals(eventTypeToHandler.get(key), simpleName)) {
            return true;
        }

        eventHandlerRepository.findByTypeAndSubTypeAndActiveTrue(event.getType(), event.getSubType())
              .ifPresent(eventHandler -> addHandlerMapping(key, eventHandler));

        return Objects.equals(eventTypeToHandler.get(key), simpleName);
    }

    protected String eventNameFor(DartsEvent dartsEvent) {
        return this.eventTypeToIdAndName.get(buildKey(dartsEvent)).getRight();
    }

    protected EventHandlerEntity eventTypeReference(DartsEvent dartsEvent) {
        var key = buildKey(dartsEvent);
        return eventHandlerRepository.getReferenceById(eventTypeToIdAndName.get(key).getLeft());
    }

    private HandlerKey buildKey(DartsEvent dartsEvent) {
        return new HandlerKey(dartsEvent.getType(), dartsEvent.getSubType());
    }

    private void addHandlerMapping(HandlerKey key, EventHandlerEntity eventType) {
        eventTypeToHandler.put(key, eventType.getHandler());
        eventTypeToIdAndName.put(key, Pair.of(eventType.getId(), eventType.getEventName()));
    }

    private record HandlerKey(String type, String subType) {}
}
