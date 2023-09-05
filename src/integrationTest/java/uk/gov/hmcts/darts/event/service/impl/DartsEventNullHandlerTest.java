package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
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

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
            .messageId("some-message-id")
            .type("nullType")
            .subType("nullSubType")
            .eventId("1")
            .courthouse(SOME_COURTHOUSE)
            .courtroom(SOME_ROOM)
            .eventText("some-text");
    }

    @Test
    void shouldDoNothingForNullMappedEvent() {
        dartsDatabase.save(someMinimalCase());
        EventHandlerEntity nullEventHandlerEntity = new EventHandlerEntity();
        nullEventHandlerEntity.setType("nullType");
        nullEventHandlerEntity.setSubType("nullSubType");
        nullEventHandlerEntity.setHandler("DartsEventNullHandler");
        nullEventHandlerEntity.setActive(true);
        nullEventHandlerEntity.setEventName("nullEventName");

        UserAccountEntity user = dartsDatabase.getUserAccountRepository().findAll().get(
            0);
        nullEventHandlerEntity.setLastModifiedBy(user);
        nullEventHandlerEntity.setCreatedBy(user);
        dartsDatabase.saveAll(nullEventHandlerEntity);

        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(today);

        EventDispatcher eventDispatcher = new EventDispatcherImpl(List.of(nullEventHandler));
        eventDispatcher.receive(event);

        Mockito.verify(nullEventHandler, Mockito.times(1)).handle(any());
    }


}
