package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.HearingCommonService;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.model.AdminGetEventById200Response;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.PatchAdminEventByIdRequest;
import uk.gov.hmcts.darts.event.service.EventService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final EventLinkedCaseRepository eventLinkedCaseRepository;
    private final HearingCommonService hearingCommonService;
    private final AuditApi auditApi;
    private final UserIdentity userIdentity;

    @Override
    public AdminGetEventById200Response adminGetEventById(Long eveId) {
        return eventMapper.mapToAdminGetEventById200Response(getEventByEveId(eveId));
    }

    @Override
    public AdminGetVersionsByEventIdResponseResult adminGetVersionsByEventId(Long eveId) {
        return eventMapper.mapToAdminGetEventVersionsResponseForId(getRelatedEvents(eveId));
    }

    @Override
    public EventEntity getEventByEveId(Long eveId) {
        return eventRepository.findById(eveId)
            .orElseThrow(() -> new DartsApiException(CommonApiError.NOT_FOUND,
                                                     String.format("Event with id %s not found", eveId)));
    }

    List<EventEntity> getRelatedEvents(Long eveId) {
        return getRelatedEvents(getEventByEveId(eveId));
    }

    List<EventEntity> getRelatedEvents(EventEntity event) {
        if (event.getEventId() == 0) {
            return List.of(event);
        }
        List<Integer> caseIds = event.getEventLinkedCaseEntities().stream()
            .map(EventLinkedCaseEntity::getCourtCase)
            .filter(Objects::nonNull)
            .map(CourtCaseEntity::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return eventRepository.findAllByRelatedEvents(
            event.getId(), event.getEventId(), caseIds);
    }

    @Override
    public EventEntity saveEvent(EventEntity eventEntity) {
        return eventRepository.save(eventEntity);
    }

    /**
     * Retrieves all event versions associated with a given court case.
     * There are 2 different queries used because migrated events are linked by courthouse and case number, and modernised events are linked by cas_id.
     *
     * @param courtCase the court case entity for which to retrieve event versions
     * @return a set of event entities associated with the given court case
     */
    @Override
    public Set<EventEntity> getAllCourtCaseEventVersions(CourtCaseEntity courtCase) {
        Set<EventEntity> allEvents = new HashSet<>();
        List<EventEntity> eventsFromCaseId = eventLinkedCaseRepository
            .findAllByCourtCase(courtCase)
            .stream().map(EventLinkedCaseEntity::getEvent).toList();
        List<EventEntity> eventsFromCaseNumber = eventLinkedCaseRepository
            .findAllByCaseNumberAndCourthouseName(courtCase.getCaseNumber(), courtCase.getCourthouse().getCourthouseName())
            .stream().map(EventLinkedCaseEntity::getEvent).toList();
        allEvents.addAll(eventsFromCaseId);
        allEvents.addAll(eventsFromCaseNumber);
        return allEvents;
    }

    @Override
    public boolean allAssociatedCasesAnonymised(EventEntity eventEntity) {
        return eventLinkedCaseRepository.areAllAssociatedCasesAnonymised(eventEntity);
    }

    @Override
    public void patchEventById(Long eveId, PatchAdminEventByIdRequest patchAdminEventByIdRequest) {
        if (!Boolean.TRUE.equals(patchAdminEventByIdRequest.getIsCurrent())) {
            throw new DartsApiException(CommonApiError.BAD_REQUEST, "is_current must be set to true");
        }
        EventEntity eventEntityToUpdate = getEventByEveId(eveId);
        if (eventEntityToUpdate.getEventId() == 0) {
            throw new DartsApiException(EventError.CAN_NOT_UPDATE_EVENT_ID_0);
        }
        if (eventEntityToUpdate.isCurrent()) {
            throw new DartsApiException(EventError.EVENT_ALREADY_CURRENT);
        }

        List<EventEntity> currentEventEntities =
            getRelatedEvents(eventEntityToUpdate)
                .stream()
                .filter(EventEntity::isCurrent) //No need to process is_current = false. These are already delinked
                .filter(eventEntity -> !eveId.equals(eventEntity.getId())) //No need to process the media entity we are updating
                .peek(this::deleteEventLinkingAndSetCurrentFalse)
                .toList();

        eventEntityToUpdate.setIsCurrent(true);
        eventRepository.save(eventEntityToUpdate);

        eventEntityToUpdate.getEventLinkedCaseEntities()
            .forEach(eventLinkedCaseEntity -> hearingCommonService.linkEventToHearings(
                eventLinkedCaseEntity.getCourtCase(),
                eventEntityToUpdate
            ));

        auditApi.record(
            AuditActivity.CURRENT_EVENT_VERSION_UPDATED,
            userIdentity.getUserAccount(),
            String.format("eve_id: %s was made current replacing eve_id: %s",
                          String.valueOf(eveId),
                          currentEventEntities.stream().map(EventEntity::getId).toList()
            ));
    }

    void deleteEventLinkingAndSetCurrentFalse(EventEntity eventEntity) {
        eventEntity.getHearingEntities().clear();
        eventEntity.setIsCurrent(false);
        eventRepository.save(eventEntity);
    }
}