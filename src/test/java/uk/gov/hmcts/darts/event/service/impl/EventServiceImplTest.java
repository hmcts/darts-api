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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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


        doReturn(event1).when(eventService).getEventEntityById(1);
        doReturn(event2).when(eventService).getEventEntityById(2);
        doReturn(event3).when(eventService).getEventEntityById(3);
        doReturn(event1).when(eventService).getEventEntityById(4);

        eventService.adminObfuscateEveByIds(List.of(1, 2, 3, 4));


        verify(dataAnonymisationService, times(1)).anonymizeEvent(event1);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event2);
        verify(dataAnonymisationService, times(1)).anonymizeEvent(event3);
        verifyNoMoreInteractions(dataAnonymisationService);

        verify(eventService, times(1)).getEventEntityById(1);
        verify(eventService, times(1)).getEventEntityById(2);
        verify(eventService, times(1)).getEventEntityById(3);
        verify(eventService, times(1)).getEventEntityById(4);
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
