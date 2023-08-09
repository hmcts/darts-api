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

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@Component
public class EventTypeToHandlerMap {

    protected final Map<String, Pair<Integer, String>> eventTypesToIdAndName = new ConcurrentHashMap<>();
    protected final Map<String, String> typeToHandler = new ConcurrentHashMap<>();

    @Autowired
    private EventHandlerRepository eventHandlerRepository;

    public boolean hasMapping(String type, String subType, String simpleName) {
        var key = buildKey(type, subType);
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

    protected String buildKey(DartsEvent dartsEvent) {
        return this.buildKey(dartsEvent.getType(), dartsEvent.getSubType());
    }

    protected String buildKey(String type, String subType) {
        requireNonNull(type);
        return type + (isNull(subType) ? "" : subType);
    }

    protected EventHandlerEntity eventTypeReference(DartsEvent dartsEvent) {
        var key = buildKey(dartsEvent.getType(), dartsEvent.getSubType());
        return eventHandlerRepository.getReferenceById(eventTypesToIdAndName.get(key).getLeft());
    }

    private void addHandlerMapping(String typeKey, EventHandlerEntity eventType) {
        typeToHandler.put(typeKey, eventType.getHandler());
        eventTypesToIdAndName.put(typeKey, Pair.of(eventType.getId(), eventType.getEventName()));
    }
}
