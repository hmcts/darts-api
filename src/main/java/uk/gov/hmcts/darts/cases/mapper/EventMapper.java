package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class EventMapper {

    public List<Event> mapResponse(List<EventEntity> eventEntities) {
        List<Event> response = new ArrayList<>();
        for (EventEntity eventEntity : eventEntities) {
            response.add(map(eventEntity));
        }
        return response;
    }

    private Event map(EventEntity eventEntity) {
        // Events can only be associated with one hearing at this stage, but will need to update this logic if that changes in future
        // e.g. single events for linked cases
        HearingEntity hearingEntity = eventEntity.getHearingEntities().get(0);

        Event event = new Event();
        event.setId(eventEntity.getId());
        event.setHearingId(hearingEntity.getId());
        event.setHearingDate(hearingEntity.getHearingDate());
        event.setTimestamp(eventEntity.getTimestamp());
        event.setName(eventEntity.getEventType().getEventName());
        event.setText(eventEntity.getEventText());

        return event;
    }

}
