package uk.gov.hmcts.darts.cases.mapper;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createEventWith;

class EventMapperTest {


    @ParameterizedTest(name = "testMapGetEventsByCaseId isAnonymised={0}")
    @ValueSource(booleans = {true, false})
    void testMapGetEventsByCaseId(boolean isAnonymised) {
        OffsetDateTime hearingDate = OffsetDateTime.parse("2024-07-01T12:00Z");
        HearingEntity hearing = CommonTestDataUtil.createHearing("case1", hearingDate.toLocalDate());
        EventEntity eventEntity = createEventWith("eventName", "event text", hearing, hearingDate);
        if (isAnonymised) {
            eventEntity.setDataAnonymised(true);
        }
        List<EventEntity> eventEntityList = Lists.newArrayList(eventEntity);
        List<Event> events = EventMapper.mapToEvents(eventEntityList);
        Event event = events.get(0);
        assertEquals(1, event.getId());
        assertEquals(102, event.getHearingId());
        assertEquals(hearingDate.toLocalDate(), event.getHearingDate());
        assertEquals("eventName", event.getName());
        assertEquals("event text", event.getText());
        assertEquals(hearingDate, event.getTimestamp());
        assertEquals(isAnonymised, event.getIsDataAnonymised());

    }

    @Test
    void testMapGetVersionedEventsByCaseId() {
        EventHandlerEntity eventType = new EventHandlerEntity();
        eventType.setEventName("TestName");

        OffsetDateTime hearingDate = OffsetDateTime.parse("2024-07-01T12:00Z");
        HearingEntity hearing = CommonTestDataUtil.createHearing("case1", hearingDate.toLocalDate());

        EventEntity eventEntity1 = createEventWith(1, 1, "Event1", hearing, eventType, hearingDate, hearingDate, false);
        EventEntity eventEntity2 = createEventWith(2, 1, "Event2", hearing, eventType, hearingDate, hearingDate.plusHours(1), true);
        EventEntity eventEntity3 = createEventWith(3, 1, "Event3", hearing, eventType, hearingDate, hearingDate.plusHours(2), false);
        EventEntity eventEntity4 = createEventWith(4, 2, "Event4", hearing, eventType, hearingDate, hearingDate, true);
        EventEntity eventEntity5 = createEventWith(5, 2, "Event5", hearing, eventType, hearingDate, hearingDate.plusHours(1), true);

        List<EventEntity> eventEntityList = Lists.newArrayList(eventEntity1, eventEntity2, eventEntity3, eventEntity4, eventEntity5);
        List<Event> events = EventMapper.mapToEvents(eventEntityList);

        assertEquals(2, events.size());
        assertEquals(2, events.get(0).getId());
        assertEquals(5, events.get(1).getId());
        
    }

}