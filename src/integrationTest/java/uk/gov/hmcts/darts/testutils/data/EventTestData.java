package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.OffsetDateTime;
import java.util.List;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class EventTestData {

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
        return event;
    }
}
