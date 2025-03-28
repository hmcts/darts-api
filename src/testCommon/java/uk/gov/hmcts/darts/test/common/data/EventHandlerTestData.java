package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;

import java.time.OffsetDateTime;

public final class EventHandlerTestData {

    private EventHandlerTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static EventHandlerEntity someMinimalEventHandler() {
        var entity = new EventHandlerEntity();
        entity.setType("some-type");
        entity.setEventName("some-desc");
        entity.setCreatedDateTime(OffsetDateTime.now());
        entity.setCreatedById(0);
        entity.setActive(true);
        entity.setReportingRestriction(false);

        return entity;
    }

    public static EventHandlerEntity createEventHandlerWith(String handlerName, String type, String subtype) {
        var minimalEventHandler = someMinimalEventHandler();
        minimalEventHandler.setHandler(handlerName);
        minimalEventHandler.setType(type);
        minimalEventHandler.setSubType(subtype);
        return minimalEventHandler;
    }

}