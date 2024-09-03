package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class EventRepositoryTest extends PostgresIntegrationBase {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStub eventStub;

    @Autowired
    private HearingStub hearingStub;

    @Test
    void testEventProcessing() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);

        List<Integer> eventIdsToBeProcessed1 = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(1));
        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        EventRepository.EventIdAndHearingIds eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.getFirst());
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            eventPkid.getEveId(), eventPkid.getEventId(), eventPkid.getHearingIds());
        Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventIdsToBeProcessed1.getFirst())));

        List<Integer> eventIdsToBeProcessed2 = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(1));
        EventRepository.EventIdAndHearingIds eventPkidSecond =
            eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed2.getFirst());
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            eventPkidSecond.getEveId(), eventPkidSecond.getEventId(), eventPkidSecond.getHearingIds());

        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        Assertions.assertTrue(eventIdMap.containsKey(eventIdsToBeProcessed2.getFirst()));
        Assertions.assertNotEquals(eventIdsToBeProcessed1, eventIdsToBeProcessed2);

        // ENSURE WE DONT PROCESS THE THIRD BATCH I.E. THE ZERO EVENT ID
        List<Integer> eventIdsToBeProcessed3 = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(1));
        Assertions.assertTrue(eventIdsToBeProcessed3.isEmpty());
    }

    @Test
    void testEventProcessingWithMultipleHearings() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);

        List<Integer> eventIdsToBeProcessed1 = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(1));
        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        List<EventEntity> eventEntities = eventIdMap.get(eventIdsToBeProcessed1.getFirst());
        EventEntity eventEntity = eventEntities.getFirst();
        eventEntity.getHearingEntities().add(
            hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(EventStub.STARTED_AT))
        );
        eventRepository.save(eventEntity);

        EventRepository.EventIdAndHearingIds eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.getFirst());
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            eventPkid.getEveId(), eventPkid.getEventId(), eventPkid.getHearingIds());
        Assertions.assertFalse(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventEntities));
    }
}