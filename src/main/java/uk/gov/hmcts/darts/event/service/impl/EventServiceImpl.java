package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.event.validation.EventIdValidator;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventIdValidator eventIdValidator;
    private final EventRepository eventRepository;

    @Override
    public AdminGetEventForIdResponseResult adminGetEventById(Integer eventId) {
        eventIdValidator.validate(eventId);
        Optional<EventEntity> eventEntityOptional = eventRepository.findById(eventId);
        return eventMapper.mapToAdminGetEventsResponseForId(eventEntityOptional);
    }

    @Override
    public EventEntity getEventEntityById(Integer eveId) {
        return eventRepository.findById(eveId)
            .orElseThrow(() -> new DartsApiException(CommonApiError.NOT_FOUND,
                                                     String.format("Event with id %s not found", eveId)));
    }

    @Override
    public EventEntity saveEvent(EventEntity eventEntity) {
        return eventRepository.save(eventEntity);
    }
}