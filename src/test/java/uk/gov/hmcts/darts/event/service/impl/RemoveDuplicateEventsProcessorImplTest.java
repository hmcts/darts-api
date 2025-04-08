package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveDuplicateEventsProcessorImplTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private CaseManagementRetentionRepository caseManagementRetentionRepository;

    @Mock
    private CaseRetentionRepository caseRetentionRepository;

    @Mock
    private EventLinkedCaseRepository eventLinkedCaseRepository;

    private RemoveDuplicateEventsProcessor removeDuplicateEventsProcessor;

    @BeforeEach
    void setUp() {
        removeDuplicateEventsProcessor = new RemoveDuplicateEventsProcessorImpl(
            eventRepository,
            eventLinkedCaseRepository,
            caseManagementRetentionRepository,
            caseRetentionRepository
        );
    }

    @Test
    void findAndRemoveDuplicateEvent_noDuplicateEvents_nothingHappens() {
        when(eventRepository.findDuplicateEventIds(any())).thenReturn(List.of());

        assertThat(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(123)).isFalse();

        verify(eventRepository).findDuplicateEventIds(123);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(caseRetentionRepository, caseManagementRetentionRepository);
    }

    @Test
    void findAndRemoveDuplicateEvent_hasDuplicateEvents_allDuplicatesExcludingTheFirstOneAreDeleted() {
        //Setup
        int duplicateEvent1Id = 1;
        int duplicateEvent2Id = 2;
        int duplicateEvent3Id = 3;

        List<Integer> duplicateEvents = new ArrayList<>(List.of(duplicateEvent1Id, duplicateEvent2Id, duplicateEvent3Id));
        when(eventRepository.findDuplicateEventIds(any())).thenReturn(duplicateEvents);

        List<Integer> caseManagementIdsToBeDeleted = List.of(2, 3);
        when(caseManagementRetentionRepository.getIdsForEvents(any()))
            .thenReturn(caseManagementIdsToBeDeleted);
        //Execution
        assertThat(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(123)).isTrue();


        //Verification
        List<Integer> toBeDeleted = List.of(duplicateEvent2Id, duplicateEvent3Id);

        verify(eventRepository).findDuplicateEventIds(123);

        verify(caseManagementRetentionRepository).getIdsForEvents(toBeDeleted);

        verify(caseRetentionRepository).deleteAllByCaseManagementIdsIn(caseManagementIdsToBeDeleted);
        verify(caseRetentionRepository).flush();
        verify(caseManagementRetentionRepository).deleteAllByEventEntityIn(toBeDeleted);
        verify(caseManagementRetentionRepository).flush();
        verify(eventLinkedCaseRepository).deleteAllByEventIn(toBeDeleted);
        verify(eventLinkedCaseRepository).flush();
        verify(eventRepository).deleteAllAssociatedHearings(toBeDeleted);
        verify(eventRepository).deleteAllById(toBeDeleted);
        verify(eventRepository).flush();
        verifyNoMoreInteractions(eventRepository);
        verifyNoMoreInteractions(caseRetentionRepository, caseManagementRetentionRepository, eventLinkedCaseRepository);
    }
}