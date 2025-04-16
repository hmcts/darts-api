package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResult;

import java.util.List;

@FunctionalInterface
public interface EventSearchService {
    List<AdminSearchEventResponseResult> searchForEvents(AdminEventSearch adminEventSearch);
}
