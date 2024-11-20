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
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;
import uk.gov.hmcts.darts.event.service.EventService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final EventLinkedCaseRepository eventLinkedCaseRepository;

    @Override
    public AdminGetEventForIdResponseResult adminGetEventById(Integer eventId) {
        return eventMapper.mapToAdminGetEventsResponseForId(getEventByEveId(eventId));
    }

    @Override
    public EventEntity getEventByEveId(Integer eveId) {
        return eventRepository.findById(eveId)
            .orElseThrow(() -> new DartsApiException(CommonApiError.NOT_FOUND,
                                                     String.format("Event with id %s not found", eveId)));
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
        return eventLinkedCaseRepository.allAssociatedCasesAnonymised(eventEntity);
    }
}