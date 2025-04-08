package uk.gov.hmcts.darts.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

@Service
@AllArgsConstructor
public class AsyncEventProcessor {
    private final CleanupCurrentFlagEventProcessor cleanupCurrentFlagEventProcessor;
    private final RemoveDuplicateEventsProcessorImpl removeDuplicateEventsProcessor;


    @Async("eventTaskExecutor")
    public void processEvent(Integer eventId, Integer eveId) {
        if (!removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(eventId, eveId)) {
            cleanupCurrentFlagEventProcessor.processEvent(eventId);
        }
    }
}
