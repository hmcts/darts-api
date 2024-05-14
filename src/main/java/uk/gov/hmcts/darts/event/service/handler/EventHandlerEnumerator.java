package uk.gov.hmcts.darts.event.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventHandlerEnumerator {

    private final List<EventHandler> eventHandlers;

    public List<String> obtainHandlers() {
        return eventHandlers.stream()
            .map(eventHandler -> ClassUtils.getUserClass(eventHandler).getSimpleName())
            .toList();
    }

}