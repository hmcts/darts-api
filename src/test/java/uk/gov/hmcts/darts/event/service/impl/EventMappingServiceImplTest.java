package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.mapper.EventHandlerMapper;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.service.handler.EventHandlerEnumerator;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMappingServiceImplTest {

    public static final OffsetDateTime FIXED_DATETIME = OffsetDateTime.of(2024, 5, 01, 10, 0, 0, 0, ZoneOffset.UTC);
    @Mock
    EventHandlerRepository eventHandlerRepository;

    @Mock
    private EventHandlerMapper eventHandlerMapper;

    @Mock
    UserAccountEntity mockUserAccountEntity;

    @Mock
    EventHandlerEnumerator eventHandlerEnumerator;

    @InjectMocks
    EventMappingServiceImpl eventMappingServiceImpl;

    @Captor
    ArgumentCaptor<EventHandlerEntity> eventHandlerEntityArgumentCaptor;

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

        eventMappingServiceImpl.postEventMapping(eventMapping, null);

        verify(eventHandlerRepository).doesActiveMappingForTypeAndSubtypeExist(anyString(), anyString());
        verify(eventHandlerRepository).saveAndFlush(eventHandlerEntityArgumentCaptor.capture());
        verifyNoMoreInteractions(eventHandlerRepository);

        EventHandlerEntity savedEventHandlerEntity = eventHandlerEntityArgumentCaptor.getValue();

        assertEquals(FIXED_DATETIME, savedEventHandlerEntity.getCreatedDateTime());
        assertEquals(eventHandlerEntity.getCreatedBy(), savedEventHandlerEntity.getCreatedBy());
    }

    @Test
    void handleRequestToSaveEventMappingForHandlerThatDoesNotExist() {
        setupHandlers();
        when(eventHandlerMapper.mapFromEventMappingAndMakeActive(any())).thenReturn(eventHandlerEntity);
        eventHandlerEntity.setHandler("Random handler");

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.postEventMapping(eventMapping, null));

        assertEquals(
            "No event handler with name Random handler could be found in the database.",
            exception.getDetail()
        );
    }

    @Test
    void handleRequestToSaveEventMappingForHandlerMappingThatAlreadyExists() {
        when(eventHandlerRepository.doesActiveMappingForTypeAndSubtypeExist(anyString(), anyString())).thenReturn(true);

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.postEventMapping(eventMapping, null));

        assertEquals(
            "Event handler mapping already exists for type: 12345 and subtype: 9876.",
            exception.getDetail()
        );
    }

    @Test
    void handleGetEventMappingRequestWhenNoEventHandlerMapping() {
        when(eventHandlerRepository.findById(anyInt())).thenReturn(Optional.empty());

        var exception = assertThrows(DartsApiException.class, () -> eventMappingServiceImpl.getEventMapping(1));

        assertEquals(
            "No event handler could be found in the database for event handler id: 1.",
            exception.getDetail()
        );
        verify(eventHandlerRepository).findById(1);
    }

    @Test
    void getEventMappingById() {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setId(1);
        eventHandlerEntity.setType("12345");
        eventHandlerEntity.setSubType("987");
        eventHandlerEntity.setEventName("Test event");
        eventHandlerEntity.setHandler("Standard Handler");
        eventHandlerEntity.setActive(true);
        eventHandlerEntity.setIsReportingRestriction(false);
        OffsetDateTime now = OffsetDateTime.now();
        eventHandlerEntity.setCreatedDateTime(now);

        when(eventHandlerRepository.findById(anyInt())).thenReturn(Optional.of(eventHandlerEntity));

        EventMapping result = eventMappingServiceImpl.getEventMapping(1);

        assertEquals(eventHandlerEntity.getId(), result.getId());
        assertEquals(eventHandlerEntity.getType(), result.getType());
        assertEquals(eventHandlerEntity.getSubType(), result.getSubType());
        assertEquals(eventHandlerEntity.getEventName(), result.getName());
        assertEquals(eventHandlerEntity.getHandler(), result.getHandler());
        assertEquals(eventHandlerEntity.getActive(), result.getIsActive());
        assertEquals(eventHandlerEntity.getIsReportingRestriction(), result.getHasRestrictions());
        assertEquals(eventHandlerEntity.getCreatedDateTime(), result.getCreatedAt());

        verify(eventHandlerRepository).findById(1);

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
        eventHandlerEntity.setIsReportingRestriction(false);
        return eventHandlerEntity;
    }

}
