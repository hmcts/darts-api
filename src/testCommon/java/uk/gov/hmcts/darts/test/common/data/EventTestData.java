package uk.gov.hmcts.darts.test.common.data;

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

    public static final int SECTION_4_1981_DB_ID = 183;
    public static final int SECTION_11_1981_DB_ID = 184;
    public static final int SECTION_39_1933_DB_ID = 185;
    public static final List<Integer> REPORTING_RESTRICTIONS_DB_IDS = List.of(54, 183, 184, 185, 186, 187, 188, 189, 190, 191);

    private static final String LOG_ENTRY_EVENT_NAME = "LOG";

    public static EventEntity someMinimalEvent() {
        return new EventEntity();
    }

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity, OffsetDateTime eventTime) {

        EventEntity event = someMinimalEvent();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventText(eventText);
        event.setTimestamp(eventTime);
        event.setIsLogEntry(LOG_ENTRY_EVENT_NAME.equals(eventName));
        event.setEventType(createTestEventHandlerEntity(eventName));
        return event;
    }

    public static int someReportingRestrictionId() {
        return REPORTING_RESTRICTIONS_DB_IDS.get(
            new Random().nextInt(REPORTING_RESTRICTIONS_DB_IDS.size()));
    }

    private EventHandlerEntity createTestEventHandlerEntity(String eventName) {
        EventHandlerEntity entity = new EventHandlerEntity();
        entity.setId(1);
        entity.setEventName(eventName);
        entity.setType("Eventtype");
        return entity;
    }

}
