package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncEventProcessorTest {
    @Mock
    private CleanupCurrentFlagEventProcessor cleanupCurrentFlagEventProcessor;

    @Mock
    private RemoveDuplicateEventsProcessorImpl removeDuplicateEventsProcessor;

    @InjectMocks
    private AsyncEventProcessor asyncEventProcessor;

    @Test
    void processEvent_hasDuplicates() {
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any()))
            .thenReturn(true);

        asyncEventProcessor.processEvent(1);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(1);
        verify(cleanupCurrentFlagEventProcessor, never()).processEvent(any());
    }

    @Test
    void processEvent_noDuplicates() {
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any()))
            .thenReturn(false);

        asyncEventProcessor.processEvent(1);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(1);
        verify(cleanupCurrentFlagEventProcessor).processEvent(1);

    }
}
