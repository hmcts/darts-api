package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.mapper.EventHandlerMapper;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.service.EventMappingService;
import uk.gov.hmcts.darts.event.service.handler.EventHandlerEnumerator;

import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NAME_DOES_NOT_EXIST;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NOT_FOUND_IN_DB;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_MAPPING_DUPLICATE_IN_DB;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventMappingServiceImpl implements EventMappingService {

    private static final String NO_HANDLER_IN_DB_MESSAGE = "No event handler could be found in the database for event handler id: %s.";
    private static final String HANDLER_ALREADY_EXISTS_MESSAGE = "Event handler mapping already exists for type: %s and subtype: %s.";

    private static final String NO_HANDLER_WITH_NAME_IN_DB_MESSAGE = "No event handler with name %s could be found in the database.";

    private final EventHandlerRepository eventHandlerRepository;
    private final EventHandlerMapper eventHandlerMapper;

    private final EventHandlerEnumerator eventHandlers;

    @Override
    public EventMapping postEventMapping(EventMapping eventMapping, Boolean isRevision) {
        if (doesActiveEventMappingExist(eventMapping.getType(), eventMapping.getSubType())) {
            throw new DartsApiException(
                EVENT_MAPPING_DUPLICATE_IN_DB,
                format(HANDLER_ALREADY_EXISTS_MESSAGE, eventMapping.getType(), eventMapping.getSubType())
            );
        } else {
            var eventHandlerEntity = eventHandlerMapper.mapFromEventMappingAndMakeActive(eventMapping);

            if (!doesEventHandlerNameExist(eventHandlerEntity.getHandler())) {
                throw new DartsApiException(
                    EVENT_HANDLER_NAME_DOES_NOT_EXIST,
                    format(NO_HANDLER_WITH_NAME_IN_DB_MESSAGE, eventHandlerEntity.getHandler())
                );
            }
            var createdEventHandler = eventHandlerRepository.saveAndFlush(eventHandlerEntity);

            return eventHandlerMapper.mapToEventMappingResponse(createdEventHandler);
        }
    }

    private boolean doesActiveEventMappingExist(String type, String subType) {
        return eventHandlerRepository.doesActiveMappingForTypeAndSubtypeExist(type, subType);
    }

    private boolean doesEventHandlerNameExist(String handlerName) {
        return eventHandlers.obtainHandlers().contains(handlerName);
    }

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
