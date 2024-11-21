package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.mapper.EventMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
    @Mock
    private EventLinkedCaseRepository eventLinkedCaseRepository;

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

    @Test
    void positiveGetAllCourtCaseEventVersions() {
        EventLinkedCaseEntity eventLinkedCase1 = new EventLinkedCaseEntity();
        EventEntity event1 = mock(EventEntity.class);
        eventLinkedCase1.setEvent(event1);
        EventLinkedCaseEntity eventLinkedCase2 = new EventLinkedCaseEntity();
        EventEntity event2 = mock(EventEntity.class);
        eventLinkedCase2.setEvent(event2);
        EventLinkedCaseEntity eventLinkedCase3 = new EventLinkedCaseEntity();
        EventEntity event3 = mock(EventEntity.class);
        eventLinkedCase3.setEvent(event3);
        when(eventLinkedCaseRepository.findAllByCourtCase(any())).thenReturn(List.of(eventLinkedCase1, eventLinkedCase2));
        when(eventLinkedCaseRepository.findAllByCaseNumberAndCourthouseName(any(), any())).thenReturn(List.of(eventLinkedCase2, eventLinkedCase3));
        CourtCaseEntity courtCase = mock(CourtCaseEntity.class, RETURNS_DEEP_STUBS);
        when(courtCase.getCaseNumber()).thenReturn("caseNumber");
        when(courtCase.getCourthouse().getCourthouseName()).thenReturn("courthouseName");

        Set<EventEntity> result = eventService.getAllCourtCaseEventVersions(courtCase);

        assertThat(result).containsExactlyInAnyOrder(event1, event2, event3);
        verify(eventLinkedCaseRepository).findAllByCourtCase(courtCase);
        verify(eventLinkedCaseRepository).findAllByCaseNumberAndCourthouseName("caseNumber", "courthouseName");
    }


    @Test
    void positiveAllAssociatedCasesAnonymisedTrue() {
        EventEntity event = mock(EventEntity.class);
        when(eventLinkedCaseRepository.areAllAssociatedCasesAnonymised(event)).thenReturn(true);
        assertThat(eventService.allAssociatedCasesAnonymised(event)).isTrue();
        verify(eventLinkedCaseRepository).areAllAssociatedCasesAnonymised(event);
    }

    @Test
    void positiveAllAssociatedCasesAnonymisedFalse() {
        EventEntity event = mock(EventEntity.class);
        when(eventLinkedCaseRepository.areAllAssociatedCasesAnonymised(event)).thenReturn(false);
        assertThat(eventService.allAssociatedCasesAnonymised(event)).isFalse();
        verify(eventLinkedCaseRepository).areAllAssociatedCasesAnonymised(event);
    }
}