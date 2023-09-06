package uk.gov.hmcts.darts.hearings.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.hearings.model.EventResponse;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods"})
public class GetEventsResponseMapper {

    public List<EventResponse> mapToEvents(List<EventEntity> eventEntities) {
        return emptyIfNull(eventEntities).stream().map(GetEventsResponseMapper::mapToEvent).collect(Collectors.toList());
    }

    public EventResponse mapToEvent(EventEntity eventEntity) {
        EventResponse eventResponse = new EventResponse();
        eventResponse.setId(eventEntity.getId());
        eventResponse.setTimestamp(eventEntity.getTimestamp());
        eventResponse.setName(eventEntity.getEventType().getEventName());
        eventResponse.setText(eventEntity.getEventText());
        return eventResponse;
    }


}
