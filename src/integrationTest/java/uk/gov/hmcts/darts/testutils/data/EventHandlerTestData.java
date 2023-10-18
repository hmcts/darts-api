package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class EventHandlerTestData {

    public static EventHandlerEntity someMinimalEventHandler() {
        var userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(0);

        var entity = new EventHandlerEntity();
        entity.setType("some-type");
        entity.setEventName("some-desc");
        entity.setCreatedDateTime(OffsetDateTime.now());
        entity.setCreatedBy(userAccountEntity);
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
