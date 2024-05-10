package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.service.EventMappingService;

import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NOT_FOUND_IN_DB;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventMappingServiceImpl implements EventMappingService {

    public static final String NO_HANDLER_IN_DB_MESSAGE = "No event handler could be found in the database for event handler id: %s.";

    private final EventHandlerRepository eventHandlerRepository;

    @Override
    public EventMapping getEventMapping(Integer id) {

        Optional<EventHandlerEntity> eventHandler = eventHandlerRepository.findById(id);

        if (eventHandler.isPresent()) {
            return mapToEventMapping(eventHandler.get());
        } else {
            log.warn(format(NO_HANDLER_IN_DB_MESSAGE, id));
            throw new DartsApiException(
                EVENT_HANDLER_NOT_FOUND_IN_DB,
                format(NO_HANDLER_IN_DB_MESSAGE, id)
            );
        }
    }

    private EventMapping mapToEventMapping(EventHandlerEntity eventEntity) {

        EventMapping mapping = new EventMapping();

        mapping.setId(eventEntity.getId());
        mapping.setType(eventEntity.getType());
        mapping.setSubType(eventEntity.getSubType());
        mapping.setName(eventEntity.getEventName());
        mapping.setHandler(eventEntity.getHandler());
        mapping.setIsActive(eventEntity.getActive());
        mapping.setHasRestrictions(eventEntity.getIsReportingRestriction());
        mapping.setCreatedAt(eventEntity.getCreatedDateTime());

        return mapping;

    }
}
