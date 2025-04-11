package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDispatcherImplTest {

    @Mock
    EventHandlerRepository eventHandlerRepository;

    @Mock
    LogApi logApi;

    @Mock
    AsyncEventProcessor asyncEventProcessor;

    @Test
    void receiveWithNoHandlers() {
        List<EventHandler> eventHandlers = new ArrayList<>();
        when(eventHandlerRepository.findByTypeAndSubType(anyString(), anyString())).thenReturn(Collections.emptyList());
        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers, eventHandlerRepository, asyncEventProcessor, logApi);


        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");
        var exception = assertThrows(DartsApiException.class, () -> eventDispatcher.receive(event));

        assertEquals(
            "No event handler could be found in the database for messageId: 1 type: TestType and subtype: TestSubType.",
            exception.getDetail()
        );
        verify(logApi, times(1)).eventReceived(event);
        verifyNoMoreInteractions(asyncEventProcessor);
    }

    @Test
    void receiveWithOneHandlers() {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        when(eventHandlerRepository.findByTypeAndSubType(anyString(), anyString())).thenReturn(List.of(eventHandlerEntity));

        List<EventHandler> eventHandlers = new ArrayList<>();
        MockEventHandler mockEventHandler = mock(MockEventHandler.class);
        when(mockEventHandler.isHandlerFor(any())).thenReturn(true);
        eventHandlers.add(mockEventHandler);
        EventEntity eventEntity = mock(EventEntity.class);
        when(eventEntity.getId()).thenReturn(321);
        when(mockEventHandler.handle(any(), any())).thenReturn(eventEntity);

        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");
        event.setEventId("123");

        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers, eventHandlerRepository, asyncEventProcessor, logApi);
        eventDispatcher.receive(event);

        verify(mockEventHandler).handle(any(), any());
        verify(logApi, times(1)).eventReceived(event);
        verify(asyncEventProcessor, times(1)).processEvent(123, 321);
    }


    private static final class MockEventHandler implements EventHandler {

        @Override
        public EventEntity handle(DartsEvent dartsEvent, EventHandlerEntity eventHandlerEntity) {
            // empty method
            return mock(EventEntity.class);
        }

        @Override
        public boolean isHandlerFor(String handlerName) {
            return true;
        }
    }
}
