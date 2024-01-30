package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class EventTestData {

    public static final int REPORTING_RESTRICTIONS_LIFTED_DB_ID = 192;
    public static final List<Integer> REPORTING_RESTRICTIONS_DB_IDS = List.of(54, 183, 184, 185, 186, 187, 188, 189, 190, 191);

    public static EventEntity someMinimalEvent() {
        return new EventEntity();
    }

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity, OffsetDateTime eventTime) {
        EventEntity event = someMinimalEvent();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventName(eventName);
        event.setEventText(eventText);
        event.setTimestamp(eventTime);
        event.setIsLogEntry(false);
        event.setEventType(createTestEventHandlerEntity());
        return event;
    }

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity, OffsetDateTime eventTime,
                                              EventHandlerEntity eventHandlerEntity) {
        EventEntity event = someMinimalEvent();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventName(eventName);
        event.setEventText(eventText);
        event.setTimestamp(eventTime);
        event.setEventType(eventHandlerEntity);
        return event;
    }

    public static int someReportingRestrictionId() {
        return REPORTING_RESTRICTIONS_DB_IDS.get(
            new Random().nextInt(REPORTING_RESTRICTIONS_DB_IDS.size()));
    }

    private EventHandlerEntity createTestEventHandlerEntity() {
        EventHandlerEntity entity = new EventHandlerEntity();
        entity.setId(1);
        entity.setEventName("Eventname");
        entity.setType("Eventtype");
        return entity;
    }

}
