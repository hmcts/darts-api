package uk.gov.hmcts.darts.hearings.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.hearings.model.EventResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
public class GetEventsResponseMapper {

    public List<EventResponse> mapToEvents(List<EventEntity> eventEntities) {
        List<EventEntity> latestEvents = filterNonLatestEvents(eventEntities);

        return emptyIfNull(latestEvents).stream()
            .map(GetEventsResponseMapper::mapToEvent)
            .collect(Collectors.toList());
    }

    public EventResponse mapToEvent(EventEntity eventEntity) {
        EventResponse eventResponse = new EventResponse();
        eventResponse.setId(eventEntity.getId());
        eventResponse.setTimestamp(eventEntity.getTimestamp());
        eventResponse.setName(eventEntity.getEventType().getEventName());
        eventResponse.setText(eventEntity.getEventText());
        eventResponse.isDataAnonymised(eventEntity.isDataAnonymised());
        return eventResponse;
    }

    public List<EventEntity> filterNonLatestEvents(List<EventEntity> events) {

        var groupedEvents =  events.stream()
            .collect(Collectors.groupingBy(event -> Optional.ofNullable(event.getEventId())));

        return groupedEvents.values().stream()
            .flatMap(GetEventsResponseMapper::getLatestEvent)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(EventEntity::getTimestamp).reversed())
            .toList();
    }

    private Stream<EventEntity> getLatestEvent(List<EventEntity> group) {
        if (group.get(0).getEventId() == null ||  group.get(0).getEventId() == 0) {
            // Issue with XHIBIT whereby they always send the first event over with ID zero. This means we end up with multiple events for a
            // case/hearing with ID zero and therefore always need to return these as we don't know which ones are genuine versions and which ones
            // are just first versions of events.
            return group.stream();
        } else {
            // Initially, all events will have isCurrent set to true until an automated task is run to clean up this field. An admin user may override this
            // value as well. Therefore, filter by is_current and if multiple exist then default to the event created date time.
            return group.stream()
                .filter(EventEntity::getIsCurrent)
                .max(Comparator.comparing(EventEntity::getCreatedDateTime)).stream();
        }
    }

}
