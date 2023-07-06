package uk.gov.hmcts.darts.event.service.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventTypeEntity;
import uk.gov.hmcts.darts.common.repository.EventTypeRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public abstract class EventHandlerBase implements EventHandler {

    protected final Map<String, Pair<Integer, String>> eventTypesToIdAndName = new ConcurrentHashMap<>();

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @PostConstruct
    public void populateMessageTypes() {
        eventTypeRepository.findByHandler(this.getClass().getSimpleName())
            .forEach(eventType -> {
                var key = buildKey(eventType.getType(), eventType.getSubType());
                eventTypesToIdAndName.put(key, Pair.of(eventType.getId(), eventType.getEventName()));
            });
    }

    @Override
    public boolean isHandlerFor(String type, String subType) {
        var key = buildKey(type, subType);
        return eventTypesToIdAndName.containsKey(key);
    }

    protected EventEntity eventEntityFrom(DartsEvent dartsEvent) {
        var event = new EventEntity();
        event.setLegacyEventId(Integer.valueOf(dartsEvent.getEventId()));
        event.setTimestamp(dartsEvent.getDateTime());
        event.setEventName(eventNameFor(dartsEvent));
        event.setEventText(dartsEvent.getEventText());
        event.setEventType(eventTypeReference(dartsEvent));
        event.setMessageId(dartsEvent.getMessageId());
        return event;
    }

    private EventTypeEntity eventTypeReference(DartsEvent dartsEvent) {
        var key = buildKey(dartsEvent.getType(), dartsEvent.getSubType());
        return eventTypeRepository.getReferenceById(eventTypesToIdAndName.get(key).getLeft());
    }

    private String eventNameFor(DartsEvent dartsEvent) {
        return this.eventTypesToIdAndName.get(buildKey(dartsEvent)).getRight();
    }

    protected String buildKey(DartsEvent dartsEvent) {
        return this.buildKey(dartsEvent.getType(), dartsEvent.getSubType());
    }

    protected String buildKey(String type, String subType) {
        requireNonNull(type);
        return type + (isNull(subType) ? "" : subType);
    }
}
