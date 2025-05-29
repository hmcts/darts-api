package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.event.model.DartsEvent;
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
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any()))
            .thenReturn(true);
        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setEventId("1");
        asyncEventProcessor.processEvent(dartsEvent);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(dartsEvent);
        verify(cleanupCurrentFlagEventProcessor, never()).processEvent(any());
    }

    @Test
    void processEvent_noDuplicates() {
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any()))
            .thenReturn(false);

        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setEventId("1");
        asyncEventProcessor.processEvent(dartsEvent);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(dartsEvent);
        verify(cleanupCurrentFlagEventProcessor).processEvent(1);
    }

    @Test
    void processEvent_noDuplicatesNullEventId() {
        when(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(any()))
            .thenReturn(false);

        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setEventId(null);
        asyncEventProcessor.processEvent(dartsEvent);

        verify(removeDuplicateEventsProcessor).findAndRemoveDuplicateEvent(dartsEvent);
        verify(cleanupCurrentFlagEventProcessor).processEvent(null);
    }
}
