package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.EventMapping;

public interface EventMappingService {
    EventMapping postEventMapping(EventMapping eventMapping, Boolean isRevision);

    EventMapping getEventMapping(Integer id);
}
