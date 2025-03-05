package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.darts.event.enums.EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;

public final class EventTestData {

    public static final int REPORTING_RESTRICTIONS_LIFTED_DB_ID = 192;

    public static final int SECTION_4_1981_DB_ID = 183;
    public static final int SECTION_11_1981_DB_ID = 184;
    public static final int SECTION_39_1933_DB_ID = 185;
    public static final List<Integer> REPORTING_RESTRICTIONS_DB_IDS = List.of(54, 183, 184, 185, 186, 187, 188, 189, 190, 191);

    private static final String LOG_ENTRY_EVENT_NAME = "LOG";

    private EventTestData() {

    }

    public static EventEntity someMinimalEvent() {
        var eventEntity = new EventEntity();
        eventEntity.setCourtroom(someMinimalCourtRoom());
        eventEntity.setEventType(createTestEventHandlerEntity("some-event-name"));
        eventEntity.setTimestamp(OffsetDateTime.now());
        eventEntity.setLogEntry(false);
        eventEntity.setIsCurrent(true);
        return eventEntity;
    }

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity, OffsetDateTime eventTime) {
        EventEntity event = someMinimalEvent();
        event.setHearingEntities(new ArrayList<>(asList(hearingEntity)));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventText(eventText);
        event.setTimestamp(eventTime);
        event.setLogEntry(LOG_ENTRY_EVENT_NAME.equals(eventName));
        event.setEventType(createTestEventHandlerEntity(eventName));
        event.setIsCurrent(true);
        event.setEventStatus(AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber());
        return event;
    }

    public static int someReportingRestrictionId() {
        return REPORTING_RESTRICTIONS_DB_IDS.get(
            new Random().nextInt(REPORTING_RESTRICTIONS_DB_IDS.size()));
    }

    private static EventHandlerEntity createTestEventHandlerEntity(String eventName) {
        EventHandlerEntity entity = new EventHandlerEntity();
        entity.setId(1);
        entity.setEventName(eventName);
        entity.setType("some-event-type");
        entity.setReportingRestriction(true);
        entity.setActive(true);
        entity.setCreatedDateTime(OffsetDateTime.now());
        return entity;
    }

    public static EventEntity createEventForHearing(HearingEntity hearingEntity) {
        var eventEntity = someMinimalEvent();
        eventEntity.getHearingEntities().add(hearingEntity);
        eventEntity.setCourtroom(hearingEntity.getCourtroom());
        return eventEntity;

    }
}