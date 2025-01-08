package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.mapper.GetEventsResponseMapper;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Slf4j
@UtilityClass
public class EventMapper {

    public List<Event> mapToEvents(List<EventEntity> eventEntities) {
        List<EventEntity> latestEvents = GetEventsResponseMapper.filterNonLatestEvents(eventEntities);

        return emptyIfNull(latestEvents).stream()
            .map(EventMapper::map)
            .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
            .collect(Collectors.toList());
    }

    private Event map(EventEntity eventEntity) {
        HearingEntity hearingEntity = eventEntity.getHearingEntities().get(0);

        Event event = new Event();
        event.setId(eventEntity.getId());
        event.setHearingId(hearingEntity.getId());
        event.setHearingDate(hearingEntity.getHearingDate());
        event.setTimestamp(eventEntity.getTimestamp());
        event.setName(eventEntity.getEventType().getEventName());
        event.setText(eventEntity.getEventText());
        event.isDataAnonymised(eventEntity.isDataAnonymised());

        return event;
    }

}