package uk.gov.hmcts.darts.event.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.event.EventTypeRepository;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public abstract class EventsHandlerBase implements EventsHandler {

    protected final Map<String, String> eventTypesToEventName = new HashMap<>();

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @PostConstruct
    public void populateMessageTypes() {
        eventTypeRepository.findByHandler(this.getClass().getSimpleName())
            .forEach(eventType -> {
                var key = buildKey(eventType.getType(), eventType.getSubType());
                eventTypesToEventName.put(key, eventType.getEventName());
            });
    }

    @Override
    public boolean isHandlerFor(String type, String subType) {
        var key = buildKey(type, subType);
        return eventTypesToEventName.containsKey(key);
    }

    protected String buildKey(String type, String subType) {
        requireNonNull(type);
        return type + (isNull(subType) ? "" : subType);
    }

}
