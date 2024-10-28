package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;

import java.util.List;

public interface EventService {
    AdminGetEventForIdResponseResult adminGetEventById(Integer eventId);

    EventEntity getEventByEveId(Integer eveId);

    EventEntity saveEvent(EventEntity eventEntity);

    List<EventEntity> getAllCourtCaseEventVersions(CourtCaseEntity courtCase);
}