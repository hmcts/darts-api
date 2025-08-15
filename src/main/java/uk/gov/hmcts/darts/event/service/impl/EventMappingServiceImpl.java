package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity_;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.mapper.EventHandlerMapper;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.service.EventMappingService;
import uk.gov.hmcts.darts.event.service.handler.EventHandlerEnumerator;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_MAPPING_INACTIVE_DELETED;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_MAPPING_INACTIVE_UPDATED;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_MAPPING_IN_USE;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NAME_DOES_NOT_EXIST;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_HANDLER_NOT_FOUND_IN_DB;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_MAPPING_DOES_NOT_EXIST_IN_DB;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_MAPPING_DUPLICATE_IN_DB;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventMappingServiceImpl implements EventMappingService {

    private static final String NO_HANDLER_IN_DB_MESSAGE = "No event handler could be found in the database for event handler id: %s.";
    private static final String HANDLER_ALREADY_EXISTS_MESSAGE = "Event handler mapping already exists for type: %s and subtype: %s.";
    private static final String HANDLER_DOES_NOT_EXIST_MESSAGE = "Event handler mapping does not exist for type: %s and subtype: %s.";
    private static final String NO_HANDLER_WITH_NAME_IN_DB_MESSAGE = "No event handler with name %s could be found in the database.";
    // {0,number,#} is to format numbers without commas
    private static final String MAPPING_IS_INACTIVE_MESSAGE = "Event handler mapping {0,number,#} cannot be deleted because it is inactive.";
    private static final String MAPPING_IS_INACTIVE_MESSAGE_UPDATE = "Event handler mapping %s cannot be updated because it is inactive.";
    private static final String MAPPING_IN_USE_MESSAGE = "Event handler mapping {0} already has processed events, so cannot be deleted.";

    private final EventRepository eventRepository;
    private final EventHandlerRepository eventHandlerRepository;
    private final EventHandlerMapper eventHandlerMapper;
    private final EventHandlerEnumerator eventHandlers;

    private final AuditApi auditApi;

    @Override
    public EventMapping postEventMapping(EventMapping eventMapping, Boolean isRevision) {
        List<EventHandlerEntity> activeMappings = getActiveMappingsForTypeAndSubtype(eventMapping.getType(), eventMapping.getSubType());

        var eventHandlerEntity = eventHandlerMapper.mapFromEventMappingAndMakeActive(eventMapping);
        validateEventHandlerExists(eventHandlerEntity.getHandler());

        if (isRevision) {
            if (!doesActiveEventMappingExist(activeMappings)) {
                throw new DartsApiException(
                    EVENT_MAPPING_DOES_NOT_EXIST_IN_DB,
                    format(HANDLER_DOES_NOT_EXIST_MESSAGE, eventMapping.getType(), eventMapping.getSubType())
                );
            }
            //Skip the check for inactive mappings if the event handler id is null as we can not verify which mapping is inactive
            if (eventMapping.getId() != null
                //Check if the event handler id is not returned it means it is inactive
                && activeMappings.stream()
                .filter(eventHandler -> eventHandler.getId().equals(eventMapping.getId()))
                .findAny().isEmpty()) {

                throw new DartsApiException(
                    EVENT_HANDLER_MAPPING_INACTIVE_UPDATED,
                    format(MAPPING_IS_INACTIVE_MESSAGE_UPDATE, eventMapping.getId())
                );
            }
            updatePreviousVersionsToInactive(activeMappings);
        } else if (doesActiveEventMappingExist(activeMappings)) {
            throw new DartsApiException(
                EVENT_MAPPING_DUPLICATE_IN_DB,
                format(HANDLER_ALREADY_EXISTS_MESSAGE, eventMapping.getType(), eventMapping.getSubType())
            );
        }

        var createdEventHandler = eventHandlerRepository.saveAndFlush(eventHandlerEntity);
        auditApi.record(AuditActivity.ADDING_EVENT_MAPPING);

        return eventHandlerMapper.mapToEventMappingResponse(createdEventHandler);
    }

    private void validateEventHandlerExists(String handlerName) {
        if (eventHandlers.obtainHandlers().contains(handlerName)) {
            return;
        }
        throw new DartsApiException(
            EVENT_HANDLER_NAME_DOES_NOT_EXIST,
            format(NO_HANDLER_WITH_NAME_IN_DB_MESSAGE, handlerName)
        );
    }

    private void updatePreviousVersionsToInactive(List<EventHandlerEntity> activeMappings) {
        for (EventHandlerEntity mapping : activeMappings) {
            mapping.setActive(false);
            auditApi.record(AuditActivity.CHANGE_EVENT_MAPPING);
        }

        eventHandlerRepository.saveAllAndFlush(activeMappings);
    }

    private boolean doesActiveEventMappingExist(List<EventHandlerEntity> activeMappings) {
        return activeMappings != null && !activeMappings.isEmpty();
    }

    private List<EventHandlerEntity> getActiveMappingsForTypeAndSubtype(String type, String subType) {
        return eventHandlerRepository.findActiveMappingsForTypeAndSubtype(type, subType);
    }

    @Override
    public List<EventMapping> getEventMappings() {
        return eventHandlerRepository.findAll(Sort.by(EventHandlerEntity_.EVENT_NAME).ascending())
            .stream()
            .map(this::mapToEventMapping)
            .toList();
    }

    @Override
    public EventMapping getEventMappingById(Integer id) {

        Optional<EventHandlerEntity> eventHandler = eventHandlerRepository.findById(id);

        if (eventHandler.isPresent()) {
            EventMapping eventMapping = mapToEventMapping(eventHandler.get());
            eventMapping.setHasEvents(eventRepository.doesEventHandlerHaveEvents(eventMapping.getId()));
            return eventMapping;
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
        mapping.setHasRestrictions(eventEntity.isReportingRestriction());
        mapping.setCreatedAt(eventEntity.getCreatedDateTime());

        return mapping;
    }

    @Override
    public void deleteEventMapping(Integer id) {
        Optional<EventHandlerEntity> eventHandlerOpt = eventHandlerRepository.findById(id);
        if (eventHandlerOpt.isEmpty()) {
            String errorMessage = format(NO_HANDLER_IN_DB_MESSAGE, id);
            log.warn(errorMessage);
            throw new DartsApiException(
                EVENT_HANDLER_NOT_FOUND_IN_DB,
                errorMessage
            );
        }

        EventHandlerEntity eventHandler = eventHandlerOpt.get();
        if (!eventHandler.getActive()) {
            String errorMessage = MessageFormat.format(MAPPING_IS_INACTIVE_MESSAGE, id);
            log.warn(errorMessage);
            throw new DartsApiException(
                EVENT_HANDLER_MAPPING_INACTIVE_DELETED,
                errorMessage
            );
        }

        boolean eventsExistForEventType = eventRepository.doesEventHandlerHaveEvents(id);
        if (eventsExistForEventType) {
            String errorMessage = MessageFormat.format(MAPPING_IN_USE_MESSAGE, id);
            log.warn(errorMessage);
            throw new DartsApiException(
                EVENT_HANDLER_MAPPING_IN_USE,
                errorMessage
            );
        }

        eventHandlerRepository.delete(eventHandler);
        auditApi.record(AuditActivity.DELETE_EVENT_MAPPING);

    }
}
