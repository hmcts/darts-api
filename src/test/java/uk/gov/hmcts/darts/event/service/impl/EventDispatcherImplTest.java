package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EventDispatcherImplTest {

    @Test
    void receiveWithMultipleHandlersForSameEvent() {
        List<EventHandler> eventHandlers = new ArrayList<>();
        eventHandlers.add(new TestEventHandler());
        eventHandlers.add(new TestEventHandler());

        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers);


        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");
        var exception = assertThrows(DartsApiException.class, () -> eventDispatcher.receive(event));

        assertEquals(
            "More than one event handler found for message: 1 type: TestType and subtype: TestSubType",
            exception.getDetail()
        );
    }

    @Test
    void receiveWithNoHandlers() {
        List<EventHandler> eventHandlers = new ArrayList<>();
        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers);


        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");
        var exception = assertThrows(DartsApiException.class, () -> eventDispatcher.receive(event));

        assertEquals(
            "No event handler could be found for message: 1 type: TestType and subtype: TestSubType",
            exception.getDetail()
        );
    }

    @Test
    void receiveWithOneHandlers() {
        List<EventHandler> eventHandlers = new ArrayList<>();
        TestEventHandler testEventHandler = Mockito.mock(TestEventHandler.class);
        Mockito.when(testEventHandler.isHandlerFor(any())).thenReturn(true);
        eventHandlers.add(testEventHandler);

        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers);

        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");

        eventDispatcher.receive(event);

        Mockito.verify(testEventHandler).handle(any());
    }


    private static class TestEventHandler implements EventHandler {

        @Override
        public void handle(DartsEvent dartsEvent) {

        }

        @Override
        public boolean isHandlerFor(DartsEvent event) {
            return true;
        }
    }
}
