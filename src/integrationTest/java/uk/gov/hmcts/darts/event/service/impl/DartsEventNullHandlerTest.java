package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.event.service.handler.DartsEventNullHandler;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

class DartsEventNullHandlerTest extends HandlerTestData {

    @SpyBean
    DartsEventNullHandler nullEventHandler;

    @Mock
    LogApi logApi;

    @Autowired
    EventHandlerRepository eventHandlerRepository;
    @Mock
    AsyncEventProcessor asyncEventProcessor;

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
            .messageId("some-message-id")
            .type("40790")
            .eventId("1")
            .courthouse(SOME_COURTHOUSE)
            .courtroom(SOME_ROOM)
            .eventText("some-text");
    }

    @Test
    void shouldDoNothingForNullMappedEvent() {
        dartsPersistence.save(PersistableFactory.getCourtCaseTestData().someMinimalCase());

        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(today);

        EventDispatcher eventDispatcher = new EventDispatcherImpl(List.of(nullEventHandler), eventHandlerRepository, asyncEventProcessor, logApi);
        eventDispatcher.receive(event);

        Mockito.verify(nullEventHandler, Mockito.times(1)).handle(any(), any());
    }


}