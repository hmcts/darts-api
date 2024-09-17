package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CleanupCurrentFlagEventProcessorImplTest extends PostgresIntegrationBase {
    private final EventRepository eventRepository;
    private final EventStub eventStub;
    private final HearingStub hearingStub;

    private CleanupCurrentFlagEventProcessorImpl cleanupCurrentFlagEventProcessor;

    @BeforeEach
    void beforeEach() {
        this.cleanupCurrentFlagEventProcessor = new CleanupCurrentFlagEventProcessorImpl(20, eventRepository);
    }

    @Test
    void givenEventCleanUpProcessor_whenVersionedEventsAreFound_thenOlderVersionsAreMarkedAsNonCurrentWhenHearingsMatch() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false);
        assertAllEventsAreCurrent(eventIdMap);
        cleanupCurrentFlagEventProcessor.processCurrentEvent();
        assertOnlyOneCurrentPerEventId(eventIdMap);
    }


    @Test
    void givenEventCleanUpProcessor_whenVersionedEventsAreFound_thenOlderVersionsAreNotMarkedAsNonCurrentWhenHearingsDoNotMatch() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        HearingEntity newHearing = hearingStub.createHearing("Bristol", "2", "case3", DateConverterUtil.toLocalDateTime(EventStub.STARTED_AT));
        EventEntity newEventEntity = eventStub.createEvent(newHearing, 10, EventStub.STARTED_AT, "LOG", 2);
        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false);
        eventIdMap.get(2).add(newEventEntity);
        assertAllEventsAreCurrent(eventIdMap);
        cleanupCurrentFlagEventProcessor.processCurrentEvent();
        assertOnlyOneCurrentPerEventId(eventIdMap, newEventEntity.getId());
    }

    private void assertOnlyOneCurrentPerEventId(Map<Integer, List<EventEntity>> eventIdMap, Integer... eveIdsToExclude) {
        List<Integer> eventIds = List.of(eveIdsToExclude);
        eventIdMap.keySet()
            .stream()
            .filter(eventId -> eventId != 0)
            .forEach(eventId -> Assertions.assertEquals(
                1, eventRepository.findAllByEventId(eventId)
                    .stream()
                    .filter(eventEntity -> {
                        if (eventIds.contains(eventEntity.getId())) {
                            assertTrue(eventEntity.getIsCurrent());
                            return false;
                        }
                        return true;
                    })
                    .filter(EventEntity::getIsCurrent)
                    .count())
            );
    }

    private void assertAllEventsAreCurrent(Map<Integer, List<EventEntity>> eventIdMap) {
        eventIdMap.values()
            .forEach(eventEntities -> eventEntities
                .forEach(eventEntity -> assertTrue(eventEntity.getIsCurrent())));
    }
}