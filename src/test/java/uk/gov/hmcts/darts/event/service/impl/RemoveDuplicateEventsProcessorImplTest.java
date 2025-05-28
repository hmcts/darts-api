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
import uk.gov.hmcts.darts.event.model.DartsEvent;
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
        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setEventId("123");
        dartsEvent.setMessageId("messageId");
        dartsEvent.setEventText("eventText");
        when(eventRepository.findDuplicateEventIds(any(), any(), any())).thenReturn(List.of());

        assertThat(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(dartsEvent)).isFalse();

        verify(eventRepository).findDuplicateEventIds(123, "messageId", "eventText");
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(caseRetentionRepository, caseManagementRetentionRepository);
    }

    @Test
    void findAndRemoveDuplicateEvent_hasDuplicateEvents_allDuplicatesExcludingTheFirstOneAreDeleted() {
        //Setup
        long duplicateEvent1Id = 1;
        long duplicateEvent2Id = 2;
        long duplicateEvent3Id = 3;

        List<Long> duplicateEvents = new ArrayList<>(List.of(duplicateEvent1Id, duplicateEvent2Id, duplicateEvent3Id));
        when(eventRepository.findDuplicateEventIds(any(), any(), any())).thenReturn(duplicateEvents);

        List<Integer> caseManagementIdsToBeDeleted = List.of(2, 3);
        when(caseManagementRetentionRepository.getIdsForEvents(any()))
            .thenReturn(caseManagementIdsToBeDeleted);

        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setEventId("123");
        dartsEvent.setMessageId("messageId");
        dartsEvent.setEventText("eventText");
        //Execution
        assertThat(removeDuplicateEventsProcessor.findAndRemoveDuplicateEvent(dartsEvent)).isTrue();


        //Verification
        List<Long> toBeDeleted = List.of(duplicateEvent2Id, duplicateEvent3Id);

        verify(eventRepository).findDuplicateEventIds(123, "messageId", "eventText");

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