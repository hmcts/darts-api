package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class EventHandlerTestData {

    public static EventHandlerEntity someMinimalEventHandler() {
        var modifiedBy = new UserAccountEntity();
        modifiedBy.setId(0);

        var entity = new EventHandlerEntity();
        entity.setType("some-type");
        entity.setEventName("some-desc");
        entity.setCreatedTimestamp(OffsetDateTime.now());
        entity.setModifiedTimestamp(OffsetDateTime.now());
        entity.setModifiedBy(modifiedBy);
        entity.setActive(true);

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
