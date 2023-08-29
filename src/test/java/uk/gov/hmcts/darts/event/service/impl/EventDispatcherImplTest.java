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
        MockEventHandler mockEventHandler = Mockito.mock(MockEventHandler.class);
        Mockito.when(mockEventHandler.isHandlerFor(any())).thenReturn(true);
        eventHandlers.add(mockEventHandler);



        DartsEvent event = new DartsEvent();
        event.setType("TestType");
        event.setSubType("TestSubType");
        event.setMessageId("1");

        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl(eventHandlers);
        eventDispatcher.receive(event);

        Mockito.verify(mockEventHandler).handle(any());
    }


    private static class MockEventHandler implements EventHandler {

        @Override
        public void handle(DartsEvent dartsEvent) {

        }

        @Override
        public boolean isHandlerFor(DartsEvent event) {
            return true;
        }
    }
}
