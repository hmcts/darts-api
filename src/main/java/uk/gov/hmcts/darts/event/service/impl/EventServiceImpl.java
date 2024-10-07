package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.event.validation.EventIdValidator;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventIdValidator eventIdValidator;
    private final EventRepository eventRepository;
    private final DataAnonymisationService dataAnonymisationService;

    @Override
    public AdminGetEventForIdResponseResult adminGetEventById(Integer eventId) {
        eventIdValidator.validate(eventId);
        Optional<EventEntity> eventEntityOptional = eventRepository.findById(eventId);
        return eventMapper.mapToAdminGetEventsResponseForId(eventEntityOptional);
    }

    @Override
    @Transactional
    public void adminObfuscateEveByIds(List<Integer> eveIds) {
        eveIds.stream()
            .map(this::getEventEntityById)
            .distinct()
            .forEach(dataAnonymisationService::anonymizeEvent);
    }

    EventEntity getEventEntityById(Integer eveId) {
        return eventRepository.findById(eveId)
            .orElseThrow(() -> new DartsApiException(DartsApiException.DartsApiErrorCommon.NOT_FOUND));
    }
}