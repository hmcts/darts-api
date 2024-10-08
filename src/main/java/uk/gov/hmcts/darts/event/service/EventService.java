package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;

public interface EventService {
    AdminGetEventForIdResponseResult adminGetEventById(Integer eventId);

    EventEntity getEventEntityById(Integer eveId);

    EventEntity saveEvent(EventEntity eventEntity);
}