package uk.gov.hmcts.darts.event.service.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

@Service
@AllArgsConstructor
public class AsyncEventProcessor {
    private final CleanupCurrentFlagEventProcessor cleanupCurrentFlagEventProcessor;
    private final RemoveDuplicateEventsProcessorImpl removeDuplicateEventsProcessor;


    @Async("eventTaskExecutor")
    public void processEvent(DartsEvent event) {
        if (!removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(event)) {
            cleanupCurrentFlagEventProcessor.processEvent(NumberUtils.createInteger(event.getEventId()));
        }
    }
}
