package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.test.common.data.EventTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventLinkedCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventRepositoryTest extends PostgresIntegrationBase {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStub eventStub;

    @Autowired
    private HearingStub hearingStub;
    @Autowired
    private EventLinkedCaseStub eventLinkedCaseStub;


    @Test
    void findDuplicateEventIds_typical() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();
        final EventEntity event3 = EventTestData.someMinimalEvent();
        final EventEntity event4 = EventTestData.someMinimalEvent(); //Not a duplicate
        event1.setEventText("eventText");
        event1.setMessageId("msgId");
        event1.setEventId(1);
        makeEventsDuplicate(event1, event2);
        makeEventsDuplicate(event1, event3);
        event4.setEventText("Some new text");
        dartsDatabase.save(event1);
        dartsDatabase.save(event2);
        dartsDatabase.save(event3);
        dartsDatabase.save(event4);
        OffsetDateTime createdTime = OffsetDateTime.now().minusDays(3);
        updateCreatedBy(event3, createdTime);
        updateCreatedBy(event1, createdTime.plusMinutes(1));
        updateCreatedBy(event2, createdTime.plusMinutes(2));
        updateCreatedBy(event4, createdTime.plusMinutes(3));

        List<Integer> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId());

        assertThat(duplicates)
            .hasSize(3)
            .anyMatch(eventEntity -> event3.getId().equals(eventEntity))
            .anyMatch(eventEntity -> event1.getId().equals(eventEntity))
            .anyMatch(eventEntity -> event2.getId().equals(eventEntity));
    }

    @Test
    void findDuplicateEventIds_multipleEventIdsWithNull_noDuplicatesShouldBeFound() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();
        final EventEntity event3 = EventTestData.someMinimalEvent();
        final EventEntity event4 = EventTestData.someMinimalEvent(); //Not a duplicate
        event1.setEventText("eventText");
        event1.setMessageId("msgId");
        event1.setEventId(null);
        makeEventsDuplicate(event1, event2);
        makeEventsDuplicate(event1, event3);
        event4.setEventText("Some new text");
        OffsetDateTime createdTime = OffsetDateTime.now().minusDays(3);
        dartsDatabase.save(event1);
        dartsDatabase.save(event2);
        dartsDatabase.save(event3);
        dartsDatabase.save(event4);
        updateCreatedBy(event1, createdTime);
        updateCreatedBy(event2, createdTime.plusMinutes(1));
        updateCreatedBy(event3, createdTime.plusMinutes(2));
        updateCreatedBy(event4, createdTime.plusMinutes(3));

        List<Integer> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId());
        assertThat(duplicates).isEmpty();
    }

    @Test
    void findDuplicateEventIds_multipleMessageIdsWithNull_noDuplicatesShouldBeFound() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();
        final EventEntity event3 = EventTestData.someMinimalEvent();
        final EventEntity event4 = EventTestData.someMinimalEvent(); //Not a duplicate
        event1.setEventText("eventText");
        event1.setMessageId(null);
        event1.setEventId(1);
        makeEventsDuplicate(event1, event2);
        makeEventsDuplicate(event1, event3);
        event4.setEventText("Some new text");
        OffsetDateTime createdTime = OffsetDateTime.now().minusDays(3);
        dartsDatabase.save(event1);
        dartsDatabase.save(event2);
        dartsDatabase.save(event3);
        dartsDatabase.save(event4);
        updateCreatedBy(event1, createdTime);
        updateCreatedBy(event2, createdTime.plusMinutes(1));
        updateCreatedBy(event3, createdTime.plusMinutes(2));
        updateCreatedBy(event4, createdTime.plusMinutes(3));

        List<Integer> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId());
        assertThat(duplicates).isEmpty();
    }

    @Test
    void findAllByRelatedEvents_shouldReturnAllRelatedEvents() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();
        final EventEntity event3 = EventTestData.someMinimalEvent();
        final EventEntity event4 = EventTestData.someMinimalEvent();
        final EventEntity event5 = EventTestData.someMinimalEvent();


        event1.setEventId(123);
        event2.setEventId(123);
        event3.setEventId(123);
        event4.setEventId(123);
        event5.setEventId(1234);

        dartsDatabase.save(event1);
        dartsDatabase.save(event2);
        dartsDatabase.save(event3);
        dartsDatabase.save(event4);
        dartsDatabase.save(event5);

        CourtCaseEntity caseEntity1 = dartsDatabase.getCourtCaseStub().createAndSaveMinimalCourtCase();
        CourtCaseEntity caseEntity2 = dartsDatabase.getCourtCaseStub().createAndSaveMinimalCourtCase();
        CourtCaseEntity caseEntity3 = dartsDatabase.getCourtCaseStub().createAndSaveMinimalCourtCase();

        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event3, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event5, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event2, caseEntity2);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event4, caseEntity3);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, caseEntity1);

        List<EventEntity> relatedEvents = eventRepository.findAllByRelatedEvents(event1.getId(), event1.getEventId(), List.of(caseEntity1.getId()));
        assertThat(relatedEvents.stream().map(EventEntity::getId).toList())
            .containsExactlyInAnyOrder(event1.getId(), event3.getId());
    }

    @Test
    void findAllByRelatedEvents_shouldReturnSingleEvent_whenEventIdIsZero() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();

        event1.setEventId(0);
        event2.setEventId(0);

        dartsDatabase.save(event1);
        dartsDatabase.save(event2);
        CourtCaseEntity caseEntity1 = dartsDatabase.getCourtCaseStub().createAndSaveMinimalCourtCase();
        eventLinkedCaseStub.createCaseLinkedEvent(event1, caseEntity1);
        eventLinkedCaseStub.createCaseLinkedEvent(event1, caseEntity1);

        List<EventEntity> relatedEvents = eventRepository.findAllByRelatedEvents(event1.getId(), event1.getEventId(), List.of(caseEntity1.getId()));
        //Only one event should be returned as the event id is 0
        assertThat(relatedEvents).hasSize(1);
        assertThat(relatedEvents.stream().map(EventEntity::getId).toList())
            .containsExactlyInAnyOrder(event1.getId());

    }

    @Test
    void findAllByEventStatusAndNotCourtrooms_shouldReturnSingleEvent_withOneValidEvent() {
        // given
        EventEntity eventWithCourtroomToBeExcluded = PersistableFactory.getEventTestData().someMinimal();
        EventEntity eventWithCourtroomToBeIncluded = PersistableFactory.getEventTestData().someMinimal();
        EventEntity eventWithDifferentEventStatus = PersistableFactory.getEventTestData().someMinimal();

        eventWithCourtroomToBeExcluded.setEventStatus(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber());
        eventWithCourtroomToBeIncluded.setEventStatus(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber());
        eventWithDifferentEventStatus.setEventStatus(EventStatus.AUDIO_LINK_NOT_DONE_HERITAGE.getStatusNumber());

        eventWithCourtroomToBeExcluded = dartsPersistence.save(eventWithCourtroomToBeExcluded);
        eventWithCourtroomToBeIncluded = dartsPersistence.save(eventWithCourtroomToBeIncluded);

        // when
        List<Integer> events = eventRepository.findAllByEventStatusAndNotCourtrooms(
            EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber(),
            List.of(eventWithCourtroomToBeExcluded.getCourtroom().getId()),
            Limit.of(5));

        // then
        assertThat(events).hasSize(1);
        assertEquals(eventWithCourtroomToBeIncluded.getId(), events.getFirst());
    }

    private void updateCreatedBy(EventEntity event, OffsetDateTime offsetDateTime) {
        event.setCreatedDateTime(offsetDateTime);
        dartsDatabase.save(event);
    }

    private void makeEventsDuplicate(EventEntity event1, EventEntity event2) {
        event2.setEventId(event1.getEventId());
        event2.setMessageId(event1.getMessageId());
        event2.setEventText(event1.getEventText());
    }
}