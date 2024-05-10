package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.EventMapping;

public interface EventMappingService {
    EventMapping getEventMapping(Integer id);
}
