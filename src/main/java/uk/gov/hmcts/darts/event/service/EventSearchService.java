package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponse;

public interface EventSearchService {
    AdminSearchEventResponse searchForEvents(AdminEventSearch adminEventSearch);
}
