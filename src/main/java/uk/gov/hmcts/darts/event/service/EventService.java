package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;

public interface EventService {
    AdminGetEventForIdResponseResult adminGetEventById(Integer eventId);
}