package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.EventMapping;

import java.util.List;

public interface EventMappingService {
    EventMapping postEventMapping(EventMapping eventMapping, Boolean isRevision);

    List<EventMapping> getEventMappings();

    EventMapping getEventMappingById(Integer id);
}
