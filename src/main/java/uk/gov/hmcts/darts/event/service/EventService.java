package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventById200Response;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.PatchAdminEventByIdRequest;

import java.util.Set;

public interface EventService {
    AdminGetEventById200Response adminGetEventById(Long eveId);

    AdminGetVersionsByEventIdResponseResult adminGetVersionsByEventId(Long eveId);

    EventEntity getEventByEveId(Long eveId);

    EventEntity saveEvent(EventEntity eventEntity);

    Set<EventEntity> getAllCourtCaseEventVersions(CourtCaseEntity courtCase);

    boolean allAssociatedCasesAnonymised(EventEntity eventEntity);

    void patchEventById(Long eventId, PatchAdminEventByIdRequest patchAdminEventByIdRequest);
}