package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CleanupCurrentFlagEventProcessorImplTest extends PostgresIntegrationBase {
    private final EventRepository eventRepository;
    private final HearingRepository hearingRepository;
    private final EventStub eventStub;
    private final HearingStub hearingStub;
    @Mock
    private UserIdentity userIdentity;

    private CleanupCurrentFlagEventProcessorImpl cleanupCurrentFlagEventProcessor;

    @BeforeEach
    void beforeEach() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(TestUtils.AUTOMATION_USER_ID);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        this.cleanupCurrentFlagEventProcessor = new CleanupCurrentFlagEventProcessorImpl(eventRepository, hearingRepository, userIdentity);
    }

    @Test
    void givenEventCleanUpProcessor_whenVersionedEventsAreFound_thenOlderVersionsAreMarkedAsNonCurrentWhenHearingsMatch() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false);
        assertAllEventsAreCurrent(eventIdMap);
        eventIdMap.keySet().forEach(eventId -> {
            cleanupCurrentFlagEventProcessor.processEvent(eventId);
            assertOnlyOneCurrentPerEventId(eventId);
        });
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
        eventIdMap.keySet().forEach(eventId -> {
            cleanupCurrentFlagEventProcessor.processEvent(eventId);
            assertOnlyOneCurrentPerEventId(eventId, newEventEntity.getId());
        });
        assertOnlyOneCurrentPerEventId(eventIdMap, newEventEntity.getId());
    }

    @Test
    void givenEventCleanUpProcessor_whenVersionedEventsAreFound_thenOlderVersionsAreMarkedAsNonCurrentWhenHearingsMatchNewEventsInsertedOutOfOrder() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false);

        //Update the created at time of the first event to be in the future to simulate out of order event inserts
        eventIdMap.keySet().forEach(
            eventId -> {
                List<EventEntity> eventEntities = eventIdMap.get(eventId);
                eventEntities.getFirst().setCreatedDateTime(OffsetDateTime.now().plusDays(1));
                dartsDatabase.save(eventEntities.getFirst());
            }
        );

        assertAllEventsAreCurrent(eventIdMap);
        eventIdMap.keySet().forEach(eventId -> {
            cleanupCurrentFlagEventProcessor.processEvent(eventId);
            assertOnlyOneCurrentPerEventId(eventId);
        });
        assertOnlyOneCurrentPerEventId(eventIdMap);
    }

    private void assertOnlyOneCurrentPerEventId(Map<Integer, List<EventEntity>> eventIdMap, Integer... eveIdsToExclude) {
        eventIdMap.keySet()
            .forEach(eventId -> assertOnlyOneCurrentPerEventId(eventId, eveIdsToExclude));
    }

    private void assertOnlyOneCurrentPerEventId(Integer eventId, Integer... eveIdsToExclude) {
        if (eventId == 0) {
            return;
        }
        List<EventEntity> eventEntities = eventRepository.findAllByEventId(eventId);
        assertOnlyOneCurrentPerEventId(eventEntities, eveIdsToExclude);
    }

    private void assertOnlyOneCurrentPerEventId(List<EventEntity> eventEntities, Integer... eveIdsToExclude) {
        List<Integer> eventIdsToExclude = List.of(eveIdsToExclude);
        assertThat(eventEntities).hasSizeGreaterThanOrEqualTo(3);
        OffsetDateTime maxCreatedDateTime = eventEntities
            .stream()
            .map(EventEntity::getCreatedDateTime)
            .max(OffsetDateTime::compareTo)
            .orElseThrow();

        List<EventEntity> currentEvents = eventEntities
            .stream()
            .filter(eventEntity -> {
                if (eventIdsToExclude.contains(eventEntity.getId())) {
                    assertTrue(eventEntity.getIsCurrent());
                    return false;
                }
                return true;
            })
            .filter(EventEntity::getIsCurrent)
            .toList();

        Assertions.assertEquals(1, currentEvents.size());
        Assertions.assertEquals(maxCreatedDateTime, currentEvents.getFirst().getCreatedDateTime());
        List<EventEntity> editedEvents = new ArrayList<>(currentEvents);
        editedEvents.remove(currentEvents.getFirst());
        editedEvents.forEach(eventEntity -> {
            assertThat(eventEntity.getLastModifiedBy().getId()).isEqualTo(TestUtils.AUTOMATION_USER_ID);
            assertThat(eventEntity.getLastModifiedDateTime())
                .isCloseTo(OffsetDateTime.now(), TestUtils.TIME_TOLERANCE);
        });
    }

    private void assertAllEventsAreCurrent(Map<Integer, List<EventEntity>> eventIdMap) {
        eventIdMap.values()
            .forEach(eventEntities -> eventEntities
                .forEach(eventEntity -> assertTrue(eventEntity.getIsCurrent())));
    }
}