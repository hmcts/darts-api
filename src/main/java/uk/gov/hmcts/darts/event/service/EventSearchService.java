package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResult;

import java.util.List;

public interface EventSearchService {
    List<AdminSearchEventResponseResult> searchForEvents(AdminEventSearch adminEventSearch);
}
