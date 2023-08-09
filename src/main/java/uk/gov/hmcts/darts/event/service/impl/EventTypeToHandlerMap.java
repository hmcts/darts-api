package uk.gov.hmcts.darts.event.service.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventTypeToHandlerMap {

    protected final Map<HandlerKey, Pair<Integer, String>> eventTypesToIdAndName = new ConcurrentHashMap<>();
    protected final Map<HandlerKey, String> typeToHandler = new ConcurrentHashMap<>();

    @Autowired
    private EventHandlerRepository eventHandlerRepository;

    public boolean hasMapping(String type, String subType, String simpleName) {
        var key = new HandlerKey(type, subType);
        if (Objects.equals(typeToHandler.get(key), simpleName)) {
            return true;
        }

        eventHandlerRepository.findByTypeAndSubTypeAndActiveTrue(type, subType)
              .ifPresent(eventHandler -> addHandlerMapping(key, eventHandler));

        return Objects.equals(typeToHandler.get(key), simpleName);
    }

    protected String eventNameFor(DartsEvent dartsEvent) {
        return this.eventTypesToIdAndName.get(buildKey(dartsEvent)).getRight();
    }

    protected EventHandlerEntity eventTypeReference(DartsEvent dartsEvent) {
        var key = buildKey(dartsEvent);
        return eventHandlerRepository.getReferenceById(eventTypesToIdAndName.get(key).getLeft());
    }

    private HandlerKey buildKey(DartsEvent dartsEvent) {
        return new HandlerKey(dartsEvent.getType(), dartsEvent.getSubType());
    }

    private void addHandlerMapping(HandlerKey key, EventHandlerEntity eventType) {
        typeToHandler.put(key, eventType.getHandler());
        eventTypesToIdAndName.put(key, Pair.of(eventType.getId(), eventType.getEventName()));
    }

    record HandlerKey(String type, String subType) {}
}
