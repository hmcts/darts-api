package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncEventProcessorTest {
    @Mock
    private CleanupCurrentFlagEventProcessor cleanupCurrentFlagEventProcessor;

    @Mock
    private RemoveDuplicateEventsProcessorImpl removeDuplicateEventsProcessor;

    @InjectMocks
    private AsyncEventProcessor asyncEventProcessor;

    @Test
    void processEvent_hasDuplicates() {
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any(), any()))
            .thenReturn(true);

        asyncEventProcessor.processEvent(1, 3);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(1, 3);
        verify(cleanupCurrentFlagEventProcessor, never()).processEvent(any());
    }

    @Test
    void processEvent_noDuplicates() {
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any(), any()))
            .thenReturn(false);

        asyncEventProcessor.processEvent(1, 3);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(1, 3);
        verify(cleanupCurrentFlagEventProcessor).processEvent(1);
    }
}
