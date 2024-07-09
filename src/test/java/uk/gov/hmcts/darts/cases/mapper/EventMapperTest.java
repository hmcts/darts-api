package uk.gov.hmcts.darts.cases.mapper;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createEventWith;

class EventMapperTest {

    @Test
    void testMapGetEventsByCaseId() {
        OffsetDateTime hearingDate = OffsetDateTime.parse("2024-07-01T12:00Z");
        HearingEntity hearing = CommonTestDataUtil.createHearing("case1", hearingDate.toLocalDate());
        List<EventEntity> eventEntityList = Lists.newArrayList(createEventWith("eventName", "event text", hearing, hearingDate));

        List<Event> events = EventMapper.mapResponse(eventEntityList);
        Event event = events.get(0);
        assertEquals(1, event.getId());
        assertEquals(102, event.getHearingId());
        assertEquals(hearingDate.toLocalDate(), event.getHearingDate());
        assertEquals("eventName", event.getName());
        assertEquals("event text", event.getText());
        assertEquals(hearingDate, event.getTimestamp());
    }

}