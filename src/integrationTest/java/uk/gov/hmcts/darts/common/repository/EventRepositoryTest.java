package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EventRepositoryTest extends PostgresIntegrationBase {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStub eventStub;

    @Autowired
    private HearingStub hearingStub;

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    public void testEventProcessing() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3);

        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(1);
        Assertions.assertEquals(1, eventEntityReturned.size());

        Integer eventPkid = eventRepository.getTheCurrentEventPrimaryKeyForEventId(eventEntityReturned.get(0));
        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventPkid, eventEntityReturned.get(0));
        Assertions.assertTrue(isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventEntityReturned.get(0))));

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


    public boolean isOnlyOneOfTheEventIdSetToCurrent(List<EventEntity> eventEntityList) {
        boolean currentFnd= false;
        for (EventEntity event : eventEntityList) {
            Optional<EventEntity> readEventEntity = eventRepository.findById(event.getId());

            if (readEventEntity.isPresent() && readEventEntity.get().getIsCurrent() && !currentFnd) {
                currentFnd = true;
            } else if (readEventEntity.isPresent() && readEventEntity.get().getIsCurrent()) {
                return false;
            }
        }

        return currentFnd;
    }
}