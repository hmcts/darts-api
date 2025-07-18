package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.mapper.EventHandlerMapper;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.service.handler.EventHandlerEnumerator;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMappingServiceImplTest {

    private static final OffsetDateTime FIXED_DATETIME = OffsetDateTime.of(2024, 5, 01, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Integer EVENT_HANDLER_ID = 123;
    @Mock
    EventHandlerRepository eventHandlerRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    private EventHandlerMapper eventHandlerMapper;

    @Mock
    UserAccountEntity mockUserAccountEntity;

    @Mock
    EventHandlerEnumerator eventHandlerEnumerator;

    @Mock
    AuditApi auditApi;

    @InjectMocks
    EventMappingServiceImpl eventMappingServiceImpl;

    @Captor
    ArgumentCaptor<EventHandlerEntity> eventHandlerEntityArgumentCaptor;

    @Captor
    ArgumentCaptor<List<EventHandlerEntity>> eventHandlerEntitiesArgumentCaptor;

    private final EventMapping eventMapping = someEventMapping();

    private final EventHandlerEntity eventHandlerEntity = someEventHandlerEntity();

    private final List<String> handlers = new ArrayList<>();

    private void setupHandlers() {
        handlers.add("DarStartHandler");
        when(eventHandlerEnumerator.obtainHandlers()).thenReturn(handlers);
    }

    @Test
    void handleRequestToSaveEventMapping() {
        setupHandlers();
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);

        eventMappingServiceImpl.postEventMapping(eventMapping, false);

        verify(eventHandlerRepository).findActiveMappingsForTypeAndSubtype(anyString(), anyString());
        verify(eventHandlerRepository).saveAndFlush(eventHandlerEntityArgumentCaptor.capture());
        verifyNoMoreInteractions(eventHandlerRepository);

        EventHandlerEntity savedEventHandlerEntity = eventHandlerEntityArgumentCaptor.getValue();

        assertEquals(FIXED_DATETIME, savedEventHandlerEntity.getCreatedDateTime());
        assertEquals(eventHandlerEntity.getCreatedById(), savedEventHandlerEntity.getCreatedById());
        verify(auditApi, times(1)).record(AuditActivity.ADDING_EVENT_MAPPING);
    }

    @Test
    void handleRequestToSaveRevisionToEventMappingAndMakePreviousRevisionInactive() {
        setupHandlers();
        eventHandlerEntity.setId(EVENT_HANDLER_ID);
        eventMapping.setId(EVENT_HANDLER_ID);
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);
        when(eventHandlerRepository.findActiveMappingsForTypeAndSubtype(anyString(), anyString())).thenReturn(List.of(eventHandlerEntity));

        eventMappingServiceImpl.postEventMapping(eventMapping, true);

        verify(eventHandlerRepository).findActiveMappingsForTypeAndSubtype(anyString(), anyString());
        verify(eventHandlerRepository).saveAndFlush(eventHandlerEntityArgumentCaptor.capture());
        verify(eventHandlerRepository).saveAllAndFlush(eventHandlerEntitiesArgumentCaptor.capture());

        verifyNoMoreInteractions(eventHandlerRepository);

        EventHandlerEntity savedEventHandlerEntity = eventHandlerEntityArgumentCaptor.getValue();

        assertEquals(FIXED_DATETIME, savedEventHandlerEntity.getCreatedDateTime());
        assertEquals(eventHandlerEntity.getCreatedById(), savedEventHandlerEntity.getCreatedById());

        List<EventHandlerEntity> updatedEVentHandlerEntities = eventHandlerEntitiesArgumentCaptor.getValue();
        for (EventHandlerEntity updatedEntity : updatedEVentHandlerEntities) {
            assertFalse(updatedEntity.getActive());
        }

        verify(auditApi, times(1)).record(AuditActivity.ADDING_EVENT_MAPPING);
        verify(auditApi, times(1)).record(AuditActivity.CHANGE_EVENT_MAPPING);
    }

    @Test
    void handleRequestToSaveEventMappingForHandlerThatDoesNotExist() {
        setupHandlers();
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);
        eventHandlerEntity.setHandler("Random handler");

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.postEventMapping(eventMapping, false));

        assertEquals(
            "No event handler with name Random handler could be found in the database.",
            exception.getDetail()
        );
    }

    @Test
    void handleRequestToSaveEventMappingForHandlerMappingThatAlreadyExistsAndIsRevisionFalse() {
        setupHandlers();
        when(eventHandlerRepository.findActiveMappingsForTypeAndSubtype(anyString(), anyString())).thenReturn(List.of(eventHandlerEntity));
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.postEventMapping(eventMapping, false));

        assertEquals(
            "Event handler mapping already exists for type: 12345 and subtype: 9876.",
            exception.getDetail()
        );
    }

    @Test
    void handleRequestToSaveEventMappingForHandlerMappingThatDoesNotAlreadyExistsAndIsRevisionTrue() {
        setupHandlers();
        when(eventHandlerRepository.findActiveMappingsForTypeAndSubtype(anyString(), anyString())).thenReturn(null);
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.postEventMapping(eventMapping, true));

        assertEquals(
            "Event handler mapping does not exist for type: 12345 and subtype: 9876.",
            exception.getDetail()
        );
    }

    @Test
    void postEventMapping_shouldError_whenEventHandlerIsInactive() {
        setupHandlers();
        eventHandlerEntity.setId(EVENT_HANDLER_ID + 1);
        eventMapping.setId(EVENT_HANDLER_ID);//Have a different ID to the active mapping
        when(eventHandlerRepository.findActiveMappingsForTypeAndSubtype(anyString(), anyString())).thenReturn(List.of(eventHandlerEntity));
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.postEventMapping(eventMapping, true));

        assertEquals(
            "Event handler mapping " + EVENT_HANDLER_ID + " cannot be updated because it is inactive.",
            exception.getDetail()
        );
        assertEquals(EventError.EVENT_HANDLER_MAPPING_INACTIVE_UPDATED, exception.getError());
    }

    @Test
    void handleGetEventMappingRequestWhenNoEventHandlerMapping() {
        when(eventHandlerRepository.findById(anyInt())).thenReturn(Optional.empty());

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.getEventMappingById(1));

        assertEquals(
            "No event handler could be found in the database for event handler id: 1.",
            exception.getDetail()
        );
        verify(eventHandlerRepository).findById(1);
    }

    @Test
    void getEventMappings() {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setId(1);
        eventHandlerEntity.setType("12345");
        eventHandlerEntity.setSubType("987");
        eventHandlerEntity.setEventName("Test event");
        eventHandlerEntity.setHandler("Standard Handler");
        eventHandlerEntity.setActive(true);
        eventHandlerEntity.setReportingRestriction(false);
        OffsetDateTime now = OffsetDateTime.now();
        eventHandlerEntity.setCreatedDateTime(now);

        EventHandlerEntity eventHandlerEntity2 = new EventHandlerEntity();

        when(eventHandlerRepository.findAll(Sort.by(EventHandlerEntity_.EVENT_NAME).ascending())).thenReturn(List.of(eventHandlerEntity, eventHandlerEntity2));

        List<EventMapping> result = eventMappingServiceImpl.getEventMappings();

        assertEquals(2, result.size());
        assertEquals(eventHandlerEntity.getId(), result.getFirst().getId());
        assertEquals(eventHandlerEntity.getType(), result.getFirst().getType());
        assertEquals(eventHandlerEntity.getSubType(), result.getFirst().getSubType());
        assertEquals(eventHandlerEntity.getEventName(), result.getFirst().getName());
        assertEquals(eventHandlerEntity.getHandler(), result.getFirst().getHandler());
        assertEquals(eventHandlerEntity.getActive(), result.getFirst().getIsActive());
        assertEquals(eventHandlerEntity.isReportingRestriction(), result.getFirst().getHasRestrictions());
        assertEquals(eventHandlerEntity.getCreatedDateTime(), result.getFirst().getCreatedAt());

        verify(eventHandlerRepository).findAll(Sort.by(EventHandlerEntity_.EVENT_NAME).ascending());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void getEventMappingById(boolean hasEvents) {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setId(1);
        eventHandlerEntity.setType("12345");
        eventHandlerEntity.setSubType("987");
        eventHandlerEntity.setEventName("Test event");
        eventHandlerEntity.setHandler("Standard Handler");
        eventHandlerEntity.setActive(true);
        eventHandlerEntity.setReportingRestriction(false);
        OffsetDateTime now = OffsetDateTime.now();
        eventHandlerEntity.setCreatedDateTime(now);

        when(eventHandlerRepository.findById(anyInt())).thenReturn(Optional.of(eventHandlerEntity));
        when(eventRepository.doesEventHandlerHaveEvents(anyInt())).thenReturn(hasEvents);

        EventMapping result = eventMappingServiceImpl.getEventMappingById(1);

        assertEquals(eventHandlerEntity.getId(), result.getId());
        assertEquals(eventHandlerEntity.getType(), result.getType());
        assertEquals(eventHandlerEntity.getSubType(), result.getSubType());
        assertEquals(eventHandlerEntity.getEventName(), result.getName());
        assertEquals(eventHandlerEntity.getHandler(), result.getHandler());
        assertEquals(eventHandlerEntity.getActive(), result.getIsActive());
        assertEquals(eventHandlerEntity.isReportingRestriction(), result.getHasRestrictions());
        assertEquals(eventHandlerEntity.getCreatedDateTime(), result.getCreatedAt());
        assertEquals(hasEvents, result.getHasEvents());

        verify(eventHandlerRepository).findById(1);

    }

    @Test
    void deleteEventMappingById() {
        eventHandlerEntity.setId(1);
        eventHandlerEntity.setType("12345");
        eventHandlerEntity.setSubType("987");
        eventHandlerEntity.setEventName("Test event");
        eventHandlerEntity.setHandler("Standard Handler");
        eventHandlerEntity.setActive(true);
        eventHandlerEntity.setReportingRestriction(false);
        OffsetDateTime now = OffsetDateTime.now();
        eventHandlerEntity.setCreatedDateTime(now);

        when(eventHandlerRepository.findById(anyInt())).thenReturn(Optional.of(eventHandlerEntity));
        when(eventRepository.doesEventHandlerHaveEvents(anyInt())).thenReturn(false);

        eventMappingServiceImpl.deleteEventMapping(1);

        verify(eventHandlerRepository).delete(eventHandlerEntity);
        verify(auditApi, times(1)).record(AuditActivity.DELETE_EVENT_MAPPING);
    }

    private EventMapping someEventMapping() {
        EventMapping eventMapping = new EventMapping();
        eventMapping.setType("12345");
        eventMapping.setSubType("9876");
        eventMapping.setHandler("DarStartHandler");
        return eventMapping;
    }

    private EventHandlerEntity someEventHandlerEntity() {

        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setCreatedDateTime(FIXED_DATETIME);
        eventHandlerEntity.setCreatedBy(mockUserAccountEntity);
        eventHandlerEntity.setType("12345");
        eventHandlerEntity.setSubType("9876");
        eventHandlerEntity.setEventName("My Event");
        eventHandlerEntity.setHandler("DarStartHandler");
        eventHandlerEntity.setActive(true);
        eventHandlerEntity.setReportingRestriction(false);
        return eventHandlerEntity;
    }

}