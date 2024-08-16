package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.util.List;
import java.util.Map;

@Disabled("Impacted by V1_364__not_null_constraints_part3.sql")
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

        Integer eventPkid = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed1.get(0));
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            List.of(eventPkid), List.of(eventIdsToBeProcessed1.get(0)));
        Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventIdsToBeProcessed1.get(0))));

        List<Integer> eventIdsToBeProcessed2 = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(1));
        Integer eventPkidSecond = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventIdsToBeProcessed2.get(0));
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
            List.of(eventPkidSecond), List.of(eventIdsToBeProcessed2.get(0)));

        Assertions.assertEquals(1, eventIdsToBeProcessed1.size());
        Assertions.assertTrue(eventIdMap.containsKey(eventIdsToBeProcessed2.get(0)));
        Assertions.assertNotEquals(eventIdsToBeProcessed1, eventIdsToBeProcessed2);

        // ENSURE WE DONT PROCESS THE THIRD BATCH I.E. THE ZERO EVENT ID
        List<Integer> eventIdsToBeProcessed3 = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(1));
        Assertions.assertTrue(eventIdsToBeProcessed3.isEmpty());
    }
}