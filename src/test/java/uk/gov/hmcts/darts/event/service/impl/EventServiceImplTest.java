package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.validation.EventIdValidator;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventIdValidator eventIdValidator;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private DataAnonymisationService dataAnonymisationService;
    @InjectMocks
    @Spy
    private EventServiceImpl eventService;


    @Test
    void positiveAdminObfuscateEveByIds() {
        EventEntity event1 = mock(EventEntity.class);
        EventEntity event2 = mock(EventEntity.class);
        EventEntity event3 = mock(EventEntity.class);
        EventEntity event4 = mock(EventEntity.class);


        doReturn(List.of(event1, event2)).when(eventService).getEventsToObfuscate(1);
        doReturn(List.of(event2, event3)).when(eventService).getEventsToObfuscate(2);
        doReturn(List.of(event4)).when(eventService).getEventsToObfuscate(3);

        eventService.adminObfuscateEveByIds(List.of(1, 2, 3));


        verify(dataAnonymisationService, times(1)).anonymizeEvent(event1);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event2);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event3);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event4);

        verify(eventService, times(1)).getEventsToObfuscate(1);
        verify(eventService, times(1)).getEventsToObfuscate(2);
        verify(eventService, times(1)).getEventsToObfuscate(3);
    }


    @Test
    void positiveGetEventsToObfuscateZeroEventId() {
        EventEntity event = mock(EventEntity.class);
        when(event.getEventId()).thenReturn(0);
        doReturn(event).when(eventService).getEventEntityById(123);

        assertThat(eventService.getEventsToObfuscate(123)).containsExactly(event);
        verify(eventService, times(1)).getEventEntityById(123);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void positiveGetEventsToObfuscateNoneZeroEventId() {
        EventEntity event1 = mock(EventEntity.class);
        EventEntity event2 = mock(EventEntity.class);
        EventEntity event3 = mock(EventEntity.class);
        EventEntity event4 = mock(EventEntity.class);

        when(event1.getEventId()).thenReturn(1234);
        doReturn(event1).when(eventService).getEventEntityById(123);
        doReturn(List.of(event2, event3, event4)).when(eventRepository).findAllByEventId(1234);

        assertThat(eventService.getEventsToObfuscate(123)).containsExactly(event2, event3, event4);

        verify(eventService, times(1)).getEventEntityById(123);
        verify(eventRepository,times(1)).findAllByEventId(1234);
    }


    @Test
    void positiveGetEventEntityById() {
        EventEntity event = mock(EventEntity.class);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        assertThat(eventService.getEventEntityById(1)).isEqualTo(event);
        verify(eventRepository, times(1)).findById(1);
    }


    @Test
    void positiveGetEventEntityByIdNotFound() {
        when(eventRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventEntityById(1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", DartsApiException.DartsApiErrorCommon.NOT_FOUND);
        verify(eventRepository, times(1)).findById(1);
    }

}
