package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.EventMapping;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMappingServiceImplTest {

    @Mock
    EventHandlerRepository eventHandlerRepository;

    @InjectMocks
    EventMappingServiceImpl eventMappingServiceImpl;

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

}
