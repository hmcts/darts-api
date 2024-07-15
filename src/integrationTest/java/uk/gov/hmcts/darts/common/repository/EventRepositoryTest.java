package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

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

        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(1);
        Assertions.assertEquals(1, eventEntityReturned.size());

        Integer eventPkid = eventRepository.getTheCurrentEventPrimaryKeyForEventId(eventEntityReturned.get(0));
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventPkid, eventEntityReturned.get(0));
        Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventEntityReturned.get(0))));

        List<Integer> eventEntityReturnedSecond = eventRepository.getCurrentEventIdsToBeProcessed(1);
        Integer eventPkidSecond = eventRepository.getTheCurrentEventPrimaryKeyForEventId(eventEntityReturnedSecond.get(0));
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventPkidSecond, eventEntityReturnedSecond.get(0));

        Assertions.assertEquals(1, eventEntityReturned.size());
        Assertions.assertTrue(eventIdMap.containsKey(eventEntityReturnedSecond.get(0)));
        Assertions.assertNotEquals(eventEntityReturned, eventEntityReturnedSecond);

        // ENSURE WE DONT PROCESS THE THIRD BATCH I.E. THE ZERO EVENT ID
        List<Integer> eventEntityReturnedThird = eventRepository.getCurrentEventIdsToBeProcessed(1);
        Assertions.assertTrue(eventEntityReturnedThird.isEmpty());
    }
}