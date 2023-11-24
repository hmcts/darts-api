package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDispatcherImplTest {

    @Mock
    EventHandlerRepository eventHandlerRepository;

    @Test
    void receiveWithNoHandlers() {
        List<EventHandler> eventHandlers = new ArrayList<>();
        when(eventHandlerRepository.findByTypeAndSubType(anyString(), anyString())).thenReturn(Collections.emptyList());
        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers, eventHandlerRepository);


        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");
        var exception = assertThrows(DartsApiException.class, () -> eventDispatcher.receive(event));

        assertEquals(
            "No event handler could be found in the database for messageId: 1 type: TestType and subtype: TestSubType.",
            exception.getDetail()
        );
    }

    @Test
    void receiveWithOneHandlers() {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        when(eventHandlerRepository.findByTypeAndSubType(anyString(), anyString())).thenReturn(List.of(eventHandlerEntity));

        List<EventHandler> eventHandlers = new ArrayList<>();
        MockEventHandler mockEventHandler = Mockito.mock(MockEventHandler.class);
        when(mockEventHandler.isHandlerFor(any())).thenReturn(true);
        eventHandlers.add(mockEventHandler);


        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");

        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers, eventHandlerRepository);
        eventDispatcher.receive(event);

        Mockito.verify(mockEventHandler).handle(any(), any());
    }


    private static class MockEventHandler implements EventHandler {

        @Override
        public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandlerEntity) {

        }

        @Override
        public boolean isHandlerFor(String handlerName) {
            return true;
        }
    }
}
