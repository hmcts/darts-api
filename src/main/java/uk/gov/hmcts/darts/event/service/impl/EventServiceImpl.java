package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;
import uk.gov.hmcts.darts.event.service.EventService;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;

    @Override
    public AdminGetEventForIdResponseResult adminGetEventById(Integer eventId) {
        return eventMapper.mapToAdminGetEventsResponseForId(getEventById(eventId));
    }

    @Override
    public EventEntity getEventById(Integer eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new DartsApiException(EventError.EVENT_ID_NOT_FOUND_RESULTS));
    }

    @Override
    public EventEntity saveEventEntity(EventEntity event) {
        return eventRepository.save(event);
    }
}