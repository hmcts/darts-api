package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.EventTestData;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventLinkedCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    void testEventProcessing() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);

        List<Integer> eventIdsToBeProcessed1 = eventRepository.findCurrentEventIdsWithDuplicates(1);
        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        EventRepository.EventIdAndHearingIds eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.getFirst())
            .getFirst();
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            eventPkid.getEveId(), eventPkid.getEventId(), eventPkid.getHearingIds(), 0);
        Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventIdsToBeProcessed1.getFirst())));

        List<Integer> eventIdsToBeProcessed2 = eventRepository.findCurrentEventIdsWithDuplicates(1);
        EventRepository.EventIdAndHearingIds eventPkidSecond =
            eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed2.getFirst())
                .getFirst();
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            eventPkidSecond.getEveId(), eventPkidSecond.getEventId(), eventPkidSecond.getHearingIds(), 0);

        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        Assertions.assertTrue(eventIdMap.containsKey(eventIdsToBeProcessed2.getFirst()));
        Assertions.assertNotEquals(eventIdsToBeProcessed1, eventIdsToBeProcessed2);

        // ENSURE WE DONT PROCESS THE THIRD BATCH I.E. THE ZERO EVENT ID
        List<Integer> eventIdsToBeProcessed3 = eventRepository.findCurrentEventIdsWithDuplicates(1);
        Assertions.assertTrue(eventIdsToBeProcessed3.isEmpty());
    }

    @Test
    void givenEventCleanUpTask_whenVersionedEventsAreFound_thenOlderVersionsAreNotMarkedAsNonCurrentWhenHearingsDoNotMatch() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);

        List<Integer> eventIdsToBeProcessed1 = eventRepository.findCurrentEventIdsWithDuplicates(1);
        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        List<EventEntity> eventEntities = eventIdMap.get(eventIdsToBeProcessed1.getFirst());
        EventEntity eventEntity = eventEntities.getFirst();
        eventEntity.getHearingEntities().add(
            hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(EventStub.STARTED_AT))
        );
        eventRepository.save(eventEntity);

        EventRepository.EventIdAndHearingIds eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.getFirst())
            .getFirst();
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            eventPkid.getEveId(), eventPkid.getEventId(), eventPkid.getHearingIds(), 0);
        Assertions.assertFalse(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventEntities));
    }


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

        List<EventEntity> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId());

        assertThat(duplicates)
            .hasSize(3)
            .anyMatch(eventEntity -> event3.getId().equals(eventEntity.getId()))
            .anyMatch(eventEntity -> event1.getId().equals(eventEntity.getId()))
            .anyMatch(eventEntity -> event2.getId().equals(eventEntity.getId()));
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

        List<EventEntity> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId());
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

        List<EventEntity> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId());
        assertThat(duplicates).isEmpty();
    }

    @Test
    void findAllByEventIdExcludingEventIdZero_whenEventIdIsSetAboveZero_willReturnVersions() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();
        event1.setEventText("eventText");
        event2.setEventText("eventText");
        event1.setEventId(1);
        event2.setEventId(1);
        dartsDatabase.save(event1);
        dartsDatabase.save(event2);

        List<EventEntity> eventVersions = eventRepository.findAllByEventIdExcludingEventIdZero(event1.getEventId());
        assertThat(eventVersions).hasSize(2);
    }

    @Test
    void findAllByEventIdExcludingEventIdZero_whenEventIdIsSetToZero_willNotReturnVersions() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        event1.setEventText("eventText");
        event1.setEventId(0);
        dartsDatabase.save(event1);

        List<EventEntity> eventVersions = eventRepository.findAllByEventIdExcludingEventIdZero(event1.getEventId());
        assertThat(eventVersions).isEmpty();
    }

    @Test
    void findAllByRelatedEvents_shouldReturnAllRelatedEvents_usingCaseNumberAndCourtHouse() {
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

        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, "caseNumber", "courthouseName");
        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, "caseNumber2", "courthouseName");
        eventLinkedCaseStub
            .createCaseLinkedEvent(event2, "caseNumber3", "courthouseName");
        eventLinkedCaseStub
            .createCaseLinkedEvent(event3, "caseNumber2", "courthouseName");
        eventLinkedCaseStub
            .createCaseLinkedEvent(event4, "caseNumber2", "courthouseName2");
        eventLinkedCaseStub
            .createCaseLinkedEvent(event5, "caseNumber", "courthouseName");

        List<EventEntity> relatedEvents = eventRepository.findAllByRelatedEvents(event1.getId());
        //Event 2 not returned as it has a different case number to ones linked to event1
        //Event 4 not returned as it has a different courthouse name to ones linked to event1
        //Event 5 not returned as it has a different event id to ones linked to event1
        assertThat(relatedEvents.stream().map(EventEntity::getId).toList())
            .hasSize(2)
            .containsExactlyInAnyOrder(event1.getId(), event3.getId());
    }

    @Test
    void findAllByRelatedEvents_shouldReturnAllRelatedEvents_usingCaseId() {
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

        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event3, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event5, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event4, caseEntity1.getCaseNumber(), caseEntity1.getCourthouse().getCourthouseName());
        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, caseEntity1);
        eventLinkedCaseStub
            .createCaseLinkedEvent(event4, "caseNumber2", "courthouseName");

        List<EventEntity> relatedEvents = eventRepository.findAllByRelatedEvents(event1.getId());
        assertThat(relatedEvents.stream().map(EventEntity::getId).toList())
            .containsExactlyInAnyOrder(event1.getId(), event3.getId(), event4.getId());
    }

    @Test
    void findAllByRelatedEvents_shouldReturnSingleEvent_whenEventIdIsZero() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        final EventEntity event2 = EventTestData.someMinimalEvent();


        event1.setEventId(0);
        event2.setEventId(0);

        dartsDatabase.save(event1);
        dartsDatabase.save(event2);

        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, "caseNumber", "courthouseName");
        eventLinkedCaseStub
            .createCaseLinkedEvent(event1, "caseNumber", "courthouseName");

        List<EventEntity> relatedEvents = eventRepository.findAllByRelatedEvents(event1.getId());
        //Only one event should be returned as the event id is 0
        assertThat(relatedEvents).hasSize(1);
        assertThat(relatedEvents.stream().map(EventEntity::getId).toList())
            .containsExactlyInAnyOrder(event1.getId());

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