package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.model.AdminGetEventById200Response;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
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
        EventEntity event = getEventByEveId(eveId);
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
}