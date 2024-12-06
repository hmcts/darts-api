package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.someMinimalEvent;

@ExtendWith(MockitoExtension.class)
class RemoveDuplicateEventsProcessorImplTest {

    private static final OffsetDateTime EARLIEST_REMOVABLE_EVENT_DATE =
        OffsetDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.now(), UTC);
    private static final int CLEAR_UP_WINDOW_DAYS = 2;
    private static final OffsetDateTime TODAY =
        OffsetDateTime.of(LocalDate.of(2024, 1, 4), LocalTime.now(), UTC);

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CaseManagementRetentionRepository caseManagementRetentionRepository;

    @Mock
    private CaseRetentionRepository caseRetentionRepository;

    @Captor
    private ArgumentCaptor<List<EventEntity>> eventsCaptorForEventDeletion;

    @Captor
    private ArgumentCaptor<List<EventEntity>> eventsCaptorForEventForCmrDeletion;

    private RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor;

    @BeforeEach
    void setUp() {
        removeDuplicateEventsProcessor = new RemoveDuplicateEventsProcessorImpl(
            EARLIEST_REMOVABLE_EVENT_DATE.toLocalDate(),
            CLEAR_UP_WINDOW_DAYS,
            eventRepository,
            currentTimeHelper,
            caseManagementRetentionRepository,
            caseRetentionRepository
        );

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(TODAY);
    }

    @Test
    void processEvent_startDateIsBeforeModStart_shouldUseModStart() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(EARLIEST_REMOVABLE_EVENT_DATE.minusDays(1));

        when(eventRepository.findDuplicateEventIds(any(), any())).thenReturn(List.of());

        removeDuplicateEventsProcessor.processEvent(123);

        verify(eventRepository).findDuplicateEventIds(123, EARLIEST_REMOVABLE_EVENT_DATE.truncatedTo(ChronoUnit.DAYS));
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(caseRetentionRepository, caseManagementRetentionRepository);
    }

    @Test
    void processEvent_startDateIsAftterModStart_shouldUseToday() {
        OffsetDateTime startDate = EARLIEST_REMOVABLE_EVENT_DATE.plusDays(CLEAR_UP_WINDOW_DAYS).plusDays(1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startDate);
        when(eventRepository.findDuplicateEventIds(any(), any())).thenReturn(List.of());

        removeDuplicateEventsProcessor.processEvent(123);

        verify(eventRepository).findDuplicateEventIds(123, startDate.minusDays(CLEAR_UP_WINDOW_DAYS));
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(caseRetentionRepository, caseManagementRetentionRepository);
    }

    @Test
    void processEvent_noDuplicateEvents_nothingHappens() {
        when(eventRepository.findDuplicateEventIds(any(), any())).thenReturn(List.of());

        removeDuplicateEventsProcessor.processEvent(123);

        verify(eventRepository).findDuplicateEventIds(123, TODAY.minusDays(CLEAR_UP_WINDOW_DAYS));
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(caseRetentionRepository, caseManagementRetentionRepository);
    }

    @Test
    void processEvent_hasDuplicateEvents_allDuplicatesExcludingTheFirstOneAreDeleted() {
        //Setup
        EventEntity duplicateEvent1 = someMinimalEvent();
        EventEntity duplicateEvent2 = someMinimalEvent();
        EventEntity duplicateEvent3 = someMinimalEvent();

        duplicateEvent1.setCreatedDateTime(TODAY.withHour(10));
        duplicateEvent2.setCreatedDateTime(TODAY.withHour(9));
        duplicateEvent3.setCreatedDateTime(TODAY.withHour(11));

        List<EventEntity> duplicateEvents = new ArrayList<>(List.of(duplicateEvent1, duplicateEvent2, duplicateEvent3));
        when(eventRepository.findDuplicateEventIds(any(), any())).thenReturn(duplicateEvents);

        List<Integer> caseManagementIdsToBeDeleted = List.of(1, 2);
        when(caseManagementRetentionRepository.getIdsForEvents(any()))
            .thenReturn(caseManagementIdsToBeDeleted);
        //Execution
        removeDuplicateEventsProcessor.processEvent(123);

        //Verification
        List<EventEntity> toBeDeleted = List.of(duplicateEvent1, duplicateEvent3);

        verify(eventRepository).findDuplicateEventIds(123, TODAY.minusDays(CLEAR_UP_WINDOW_DAYS));

        verify(caseManagementRetentionRepository).getIdsForEvents(toBeDeleted);

        verify(caseRetentionRepository).deleteAllByCaseManagementIdsIn(caseManagementIdsToBeDeleted);
        verify(caseRetentionRepository).flush();
        verify(caseManagementRetentionRepository).deleteAllByEventEntityIn(toBeDeleted);
        verify(caseManagementRetentionRepository).flush();
        verify(eventRepository).deleteAll(toBeDeleted);
        verify(eventRepository).flush();
        verifyNoMoreInteractions(eventRepository);
        verifyNoMoreInteractions(caseRetentionRepository, caseManagementRetentionRepository);
    }
}