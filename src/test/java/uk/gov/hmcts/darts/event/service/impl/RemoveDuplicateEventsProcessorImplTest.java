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
            eventRepository,
            currentTimeHelper,
            caseManagementRetentionRepository,
            caseRetentionRepository
        );
    }

    @Test
    void processEvent_noDuplicateEvents_nothingHappens() {
        when(eventRepository.findDuplicateEventIds(any())).thenReturn(List.of());

        removeDuplicateEventsProcessor.processEvent(123);

        verify(eventRepository).findDuplicateEventIds(123);
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
        when(eventRepository.findDuplicateEventIds(any())).thenReturn(duplicateEvents);

        List<Integer> caseManagementIdsToBeDeleted = List.of(2, 3);
        when(caseManagementRetentionRepository.getIdsForEvents(any()))
            .thenReturn(caseManagementIdsToBeDeleted);
        //Execution
        removeDuplicateEventsProcessor.processEvent(123);

        //Verification
        List<EventEntity> toBeDeleted = List.of(duplicateEvent2, duplicateEvent3);

        verify(eventRepository).findDuplicateEventIds(123);

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