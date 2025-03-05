package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class EventHandlerTestData {

    private EventHandlerTestData() {

    }

    public static EventHandlerEntity someMinimalEventHandler() {
        var entity = new EventHandlerEntity();
        entity.setType("some-type");
        entity.setEventName("some-desc");
        entity.setCreatedDateTime(OffsetDateTime.now());
        entity.setCreatedBy(minimalUserAccount());
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