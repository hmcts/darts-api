package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.mapper.EventMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private DataAnonymisationService dataAnonymisationService;

    @InjectMocks
    @Spy
    private EventServiceImpl eventService;


    @Test
    void positiveGetEventEntityById() {
        EventEntity event = mock(EventEntity.class);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        assertThat(eventService.getEventByEveId(1)).isEqualTo(event);
        verify(eventRepository, times(1)).findById(1);
    }


    @Test
    void positiveGetEventEntityByIdNotFound() {
        when(eventRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventByEveId(1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", CommonApiError.NOT_FOUND);
        verify(eventRepository, times(1)).findById(1);
    }

    @Test
    void positiveSaveEvent() {
        EventEntity event = mock(EventEntity.class);
        when(eventRepository.save(event)).thenReturn(event);
        assertThat(eventService.saveEvent(event)).isEqualTo(event);
        verify(eventRepository, times(1)).save(event);
    }

}
