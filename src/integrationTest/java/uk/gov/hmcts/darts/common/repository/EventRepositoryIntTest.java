package uk.gov.hmcts.darts.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.event.model.EventSearchResult;
import uk.gov.hmcts.darts.event.service.impl.AdminEventsSearchGivensBuilder;
import uk.gov.hmcts.darts.test.common.data.EventTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventLinkedCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class EventRepositoryIntTest extends PostgresIntegrationBase {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStub eventStub;

    @Autowired
    private HearingStub hearingStub;
    @Autowired
    private EventLinkedCaseStub eventLinkedCaseStub;

    @Autowired
    private AdminEventsSearchGivensBuilder given;

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2020-06-20T10:00Z");

    @Test
    void eventProcessing_ShouldExcludeZeroEventAndDuplicates() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);
        List<EventEntity> allEvents = getEventsForEventIds(eventIdMap.keySet().stream().toList());
        assertEquals(7, allEvents.size());

        List<Integer> eventIdsToBeProcessed1 = eventIdMap.keySet().stream()
            .filter(eventId -> eventId != 0)
            .limit(1)
            .toList();

        assertEquals(1, eventIdsToBeProcessed1.size());
        EventRepository.EventIdAndHearingIds eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.getFirst())
            .getFirst();
        eventIdMap.get(eventIdsToBeProcessed1.getFirst()).forEach(event -> {
            if (!event.getId().equals(eventPkid.getEveId())) {
                event.setIsCurrent(false);
                dartsDatabase.save(event);
            }
        });

        assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventIdsToBeProcessed1.getFirst())));

        List<Integer> eventIdsToBeProcessed2 = eventIdMap.keySet().stream()
            .filter(eventId -> eventId != 0)
            .skip(1)
            .toList();

        EventRepository.EventIdAndHearingIds eventPkidSecond =
            eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed2.getFirst())
                .getFirst();

        eventIdMap.get(eventIdsToBeProcessed2.getFirst()).forEach(event -> {
            if (!event.getId().equals(eventPkidSecond.getEveId())) {
                event.setIsCurrent(false);
                dartsDatabase.save(event);
            }
        });

        assertEquals(1, eventIdsToBeProcessed1.size());
        assertTrue(eventIdMap.containsKey(eventIdsToBeProcessed2.getFirst()));
        Assertions.assertNotEquals(eventIdsToBeProcessed1, eventIdsToBeProcessed2);

        List<EventEntity> eventIdsToBeProcessed3 = eventRepository.findAll().stream()
            .filter(event -> event.getId() == 0)
            .toList();

        assertTrue(eventIdsToBeProcessed3.isEmpty());
    }

    @Test
    void givenEventCleanUpTask_whenVersionedEventsAreFound_thenOlderVersionsAreNotMarkedAsNonCurrentWhenHearingsDoNotMatch() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);

        List<Integer> eventIdsToBeProcessed1 = eventIdMap.keySet().stream()
            .filter(eventId -> eventId != 0)
            .limit(1)
            .toList();

        assertEquals(1, eventIdsToBeProcessed1.size());
        List<EventEntity> eventEntities = eventIdMap.get(eventIdsToBeProcessed1.getFirst());
        EventEntity eventEntity = eventEntities.getFirst();
        eventEntity.getHearingEntities().add(
            hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(EventStub.STARTED_AT))
        );
        eventRepository.save(eventEntity);

        EventRepository.EventIdAndHearingIds eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.getFirst())
            .getFirst();

        eventEntities.forEach(event -> {
            if (!event.getId().equals(eventPkid.getEveId())) {
                event.setIsCurrent(false);
                dartsDatabase.save(event);
            }
        });
        List<EventEntity> events = eventRepository.findAll().stream().toList();

        assertFalse(eventStub.isOnlyOneOfTheEventIdSetToCurrent(events));
    }

    @Test
    void findDuplicateEventIds_typical() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        event1.setEventText("eventText");
        event1.setMessageId("msgId");
        event1.setEventId(1);
        assertFindDuplicateEventIdsUsingPreBuiltEvent(event1);
    }

    @Test
    void findDuplicateEventIds_shouldFindDuplicatesWithNullEventText() {
        final EventEntity event1 = EventTestData.someMinimalEvent();
        event1.setEventText(null);
        event1.setMessageId("msgId");
        event1.setEventId(1);
        assertFindDuplicateEventIdsUsingPreBuiltEvent(event1);
    }

    private void assertFindDuplicateEventIdsUsingPreBuiltEvent(EventEntity event1) {
        final EventEntity event2 = EventTestData.someMinimalEvent();
        final EventEntity event3 = EventTestData.someMinimalEvent();
        final EventEntity event4 = EventTestData.someMinimalEvent(); //Not a duplicate
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

        List<Long> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId(), event1.getMessageId(), event1.getEventText());

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

        List<Long> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId(), event1.getMessageId(), event1.getEventText());
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

        List<Long> duplicates = eventRepository.findDuplicateEventIds(event1.getEventId(), event1.getMessageId(), event1.getEventText());
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
        List<Long> events = eventRepository.findAllByEventStatusAndNotCourtrooms(
            EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber(),
            List.of(eventWithCourtroomToBeExcluded.getCourtroom().getName()),
            Limit.of(5));

        // then
        assertThat(events).hasSize(1);
        assertEquals(eventWithCourtroomToBeIncluded.getId(), events.getFirst());
    }


    @Test
    void deleteAllAssociatedHearings_shouldOnlyDeleteAssociatedHearingsForProvidedIds() {
        final EventEntity event1HasHearingsIncluded = EventTestData.someMinimalEvent();
        final EventEntity event2HasHearingsIncluded = EventTestData.someMinimalEvent();
        final EventEntity event3HasHearingsExcluded = EventTestData.someMinimalEvent();
        final EventEntity event4NoHearingsIncluded = EventTestData.someMinimalEvent();

        final HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimal();
        final HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimal();
        final HearingEntity hearing3 = PersistableFactory.getHearingTestData().someMinimal();

        dartsDatabase.save(hearing1);
        dartsDatabase.save(hearing2);
        dartsDatabase.save(hearing3);

        event1HasHearingsIncluded.setHearingEntities(Set.of(hearing1, hearing2, hearing3));
        event2HasHearingsIncluded.setHearingEntities(Set.of(hearing1, hearing2));
        event3HasHearingsExcluded.setHearingEntities(Set.of(hearing3, hearing1));

        dartsDatabase.save(event1HasHearingsIncluded);
        dartsDatabase.save(event2HasHearingsIncluded);
        dartsDatabase.save(event3HasHearingsExcluded);
        dartsDatabase.save(event4NoHearingsIncluded);

        eventRepository.deleteAllAssociatedHearings(List.of(
            event1HasHearingsIncluded.getId(),
            event2HasHearingsIncluded.getId(),
            event4NoHearingsIncluded.getId()
        ));

        dartsDatabase.getTransactionalUtil().executeInTransaction(() -> {
            EventEntity afterEventEntity1 = eventRepository.findById(event1HasHearingsIncluded.getId()).orElseThrow();
            assertThat(afterEventEntity1.getHearingEntities()).isEmpty();
            EventEntity afterEventEntity2 = eventRepository.findById(event2HasHearingsIncluded.getId()).orElseThrow();
            assertThat(afterEventEntity2.getHearingEntities()).isEmpty();
            EventEntity afterEventEntity3 = eventRepository.findById(event3HasHearingsExcluded.getId()).orElseThrow();
            assertThat(afterEventEntity3.getHearingEntities().stream().map(HearingEntity::getId).toList()
            ).isNotEmpty().contains(hearing1.getId(), hearing3.getId());
            EventEntity afterEventEntity4 = eventRepository.findById(event4NoHearingsIncluded.getId()).orElseThrow();
            assertThat(afterEventEntity4.getHearingEntities()).isEmpty();
        });
    }

    @Test
    void searchEventsFilteringOn_ReturnsMatchEvents_WithNoSearchCriteria() {

        // Create and save courthouses with consistent display names
        var courthouse1 = PersistableFactory.getCourthouseTestData().someMinimal();
        courthouse1.setDisplayName("SOME-COURTHOUSE-1111");
        var courthouse2 = PersistableFactory.getCourthouseTestData().someMinimal();
        courthouse2.setDisplayName("SOME-COURTHOUSE-2222");
        var courthouse3 = PersistableFactory.getCourthouseTestData().someMinimal();
        courthouse3.setDisplayName("SOME-COURTHOUSE-3333");

        dartsDatabase.save(courthouse1);
        dartsDatabase.save(courthouse2);
        dartsDatabase.save(courthouse3);

        // Create and save courtrooms linked to courthouses
        var courtroom1 = PersistableFactory.getCourtroomTestData().someMinimal();
        courtroom1.setCourthouse(courthouse1);
        var courtroom2 = PersistableFactory.getCourtroomTestData().someMinimal();
        courtroom2.setCourthouse(courthouse2);
        var courtroom3 = PersistableFactory.getCourtroomTestData().someMinimal();
        courtroom3.setCourthouse(courthouse3);

        dartsDatabase.save(courtroom1);
        dartsDatabase.save(courtroom2);
        dartsDatabase.save(courtroom3);

        // Create and save hearings
        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimal();
        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimal();
        HearingEntity hearing3 = PersistableFactory.getHearingTestData().someMinimal();

        dartsDatabase.save(hearing1);
        dartsDatabase.save(hearing2);
        dartsDatabase.save(hearing3);

        hearing1.setCourtroom(courtroom1);
        hearing2.setCourtroom(courtroom2);
        hearing3.setCourtroom(courtroom3);
        dartsDatabase.saveAll(hearing1, hearing2, hearing3);

        // Create and save events linked to courtrooms and hearings
        EventEntity event1 = EventTestData.someMinimalEvent();
        event1.setCourtroom(courtroom1);
        event1.setHearingEntities(Set.of(hearing1));

        EventEntity event2 = EventTestData.someMinimalEvent();
        event2.setCourtroom(courtroom2);
        event2.setHearingEntities(Set.of(hearing2));

        EventEntity event3 = EventTestData.someMinimalEvent();
        event3.setCourtroom(courtroom3);
        event3.setHearingEntities(Set.of(hearing3));

        var persistedEvents = List.of(
            dartsDatabase.save(event1),
            dartsDatabase.save(event2),
            dartsDatabase.save(event3)
        );

        // Sort events by courthouse display name
        var mutablePersistedEvents = new ArrayList<>(persistedEvents);
        mutablePersistedEvents.sort((ev1, ev2) -> ev1.getCourtroom().getCourthouse().getDisplayName().compareTo(
            ev2.getCourtroom().getCourthouse().getDisplayName()));

        // Call the repository method
        List<EventSearchResult> eventSearchResults = eventRepository.searchEventsFilteringOn(
            null,
            null,
            null,
            null,
            null,
            Limit.of(10)
        );

        // Assert the results
        assertThat(eventSearchResults).hasSize(3);
        assertThat(eventSearchResults)
            .extracting(EventSearchResult::courtHouseDisplayName)
            .isEqualTo(mutablePersistedEvents.stream()
                           .map(event -> event.getCourtroom().getCourthouse().getDisplayName())
                           .toList());
    }

    @Test
    void findByCourthouseAndCourtroomBetweenStartAndEnd_shouldFindEventEntities_whenEventIsCurrent() {
        HearingEntity hearingEntity = PersistableFactory.getHearingTestData().someMinimal();
        dartsPersistence.saveAll(hearingEntity);

        final EventEntity eventEntity1 = dartsDatabase.createEvent(hearingEntity, 54);
        final EventEntity eventEntity2 = dartsDatabase.createEvent(hearingEntity, 54);
        final EventEntity eventEntity3 = dartsDatabase.createEvent(hearingEntity, 32);
        final EventEntity eventEntity4 = dartsDatabase.createEvent(hearingEntity, 68);
        final EventEntity eventEntity5 = dartsDatabase.createEvent(hearingEntity, 188);
        eventEntity5.setIsCurrent(false);

        dartsDatabase.saveAll(eventEntity1, eventEntity2, eventEntity3, eventEntity4, eventEntity5);

        // when
        List<EventEntity> resultEvents = eventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
            hearingEntity.getCourtroom().getCourthouse().getCourthouseName(),
            hearingEntity.getCourtroom().getName(),
            SOME_DATE_TIME.minusHours(1),
            SOME_DATE_TIME.plusHours(1)
        );

        // then
        assertFalse(resultEvents.isEmpty());
        assertEquals(resultEvents.size(), 4);
        List.of(eventEntity1, eventEntity2, eventEntity3, eventEntity4).forEach(event -> {
            event.equals(resultEvents.stream().filter(
                resultEvent -> resultEvent.getId().equals(event.getId())).findFirst().get()
            );
        });
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

    private List<EventEntity> getEventsForEventIds(List<Integer> eventIds) {
        List<EventEntity> events = new ArrayList<>();
        for (Integer eventId : eventIds) {
            List<EventEntity> eventEntities = eventRepository.findAllByEventId(eventId);
            events.addAll(eventEntities);
        }
        return events;
    }

}