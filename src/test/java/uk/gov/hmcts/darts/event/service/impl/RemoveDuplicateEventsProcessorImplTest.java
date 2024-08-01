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
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.someMinimalEvent;

@ExtendWith(MockitoExtension.class)
class RemoveDuplicateEventsProcessorImplTest {

    private static final LocalDate EARLIEST_REMOVABLE_EVENT_DATE = LocalDate.parse("2024-01-01");
    private static final int CLEAR_UP_WINDOW_DAYS = 2;
    private static final LocalDate TODAY = LocalDate.parse("2024-01-04");

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
            EARLIEST_REMOVABLE_EVENT_DATE,
            CLEAR_UP_WINDOW_DAYS,
            eventRepository,
            currentTimeHelper,
            caseManagementRetentionRepository,
            caseRetentionRepository
        );

        when(currentTimeHelper.currentLocalDate()).thenReturn(TODAY);
    }

    @Test
    void doesntRemoveDuplicatesEventsBeforeEarliestRemovableEventDate() {
        when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.parse("2024-01-02"));
        when(eventRepository.findAllBetweenDateTimesInclusive(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(List.of());

        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();

        verify(eventRepository, times(1))
            .findAllBetweenDateTimesInclusive(
                OffsetDateTime.parse("2024-01-01T00:00:00+00:00"),
                OffsetDateTime.parse("2024-01-02T23:59:00+00:00"));

        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void doesntRemoveDuplicateEventsBeforeClearUpWindow() {
        when(eventRepository.findAllBetweenDateTimesInclusive(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(List.of());

        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();

        verify(eventRepository, times(1))
            .findAllBetweenDateTimesInclusive(
                OffsetDateTime.parse("2024-01-02T00:00:00+00:00"),
                OffsetDateTime.parse("2024-01-04T23:59:00+00:00"));

        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void removesDuplicateEventsKeepingTheLatest() {
        when(eventRepository.findAllBetweenDateTimesInclusive(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(threeEventsEachWithTwoDuplicates());

        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();

        verify(eventRepository).deleteAllInBatch(eventsCaptorForEventDeletion.capture());
        assertThat(eventsCaptorForEventDeletion.getValue())
            .extracting("id")
            .containsOnly(0, 1, 2, 3, 4, 5);
    }

    @Test
    void removesDuplicateEventsWhenTheyAreAssociatedWithCaseRetention() {
        when(caseManagementRetentionRepository.getIdsForEvents(any())).thenReturn(List.of(1, 2, 3));
        when(eventRepository.findAllBetweenDateTimesInclusive(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(threeEventsEachWithTwoDuplicates());

        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();

        verify(caseRetentionRepository).deleteAllByCaseManagementIdsIn(List.of(1, 2, 3));
        verify(caseManagementRetentionRepository).deleteAllByEventEntityIn(eventsCaptorForEventForCmrDeletion.capture());
        verify(eventRepository).deleteAllInBatch(eventsCaptorForEventDeletion.capture());
        assertThat(eventsCaptorForEventDeletion.getValue())
            .extracting("id")
            .containsOnly(0, 1, 2, 3, 4, 5);
        assertThat(eventsCaptorForEventForCmrDeletion.getValue())
            .extracting("id")
            .containsOnly(0, 1, 2, 3, 4, 5);
    }

    @Test
    void removesDuplicateEventsWhenFoundDuplicatedAboveChunkSize() {
        when(eventRepository.findAllBetweenDateTimesInclusive(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(manyEventsEachWithTwoDuplicates());

        removeDuplicateEventsProcessor.processRemoveDuplicateEvents();

        verify(eventRepository, times(10)).deleteAllInBatch(eventsCaptorForEventDeletion.capture());
        assertThat(eventsCaptorForEventDeletion.getAllValues().stream().flatMap(List::stream))
            .extracting("id")
            .hasSameElementsAs(integersInRange(0, 9995));
    }

    private List<Integer> integersInRange(int start, int end) {
        return rangeClosed(start, end).boxed().collect(toList());
    }

    private static List<EventEntity> threeEventsEachWithTwoDuplicates() {
        return range(0, 9).mapToObj(i -> someDuplicateEvent(i)).collect(toList());
    }

    private static List<EventEntity> manyEventsEachWithTwoDuplicates() {
        return range(0, 9999).mapToObj(i -> someDuplicateEvent(i)).collect(toList());
    }

    private static EventEntity someDuplicateEvent(int quantity) {
        var eventEntity = someMinimalEvent();
        eventEntity.setId(quantity);
        eventEntity.setEventId(quantity % 3);
        eventEntity.setMessageId("message-id-" + quantity % 3);
        eventEntity.setEventText("some-event-text-" + quantity % 3);
        eventEntity.setCreatedDateTime(OffsetDateTime.now().plusSeconds(quantity));
        return eventEntity;
    }
}