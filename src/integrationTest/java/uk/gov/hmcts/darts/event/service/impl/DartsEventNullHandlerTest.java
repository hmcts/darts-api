package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

class DartsEventNullHandlerTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "";
    private static final String SOME_ROOM = "some-room";
    private final OffsetDateTime today = now();

    @SpyBean
    DartsEventNullHandler nullEventHandler;

    @Autowired
    EventHandlerRepository eventHandlerRepository;

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
        dartsDatabase.save(someMinimalCase());

        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(today);

        EventDispatcher eventDispatcher = new EventDispatcherImpl(List.of(nullEventHandler), eventHandlerRepository);
        eventDispatcher.receive(event);

        Mockito.verify(nullEventHandler, Mockito.times(1)).handle(any(), any());
    }


}
