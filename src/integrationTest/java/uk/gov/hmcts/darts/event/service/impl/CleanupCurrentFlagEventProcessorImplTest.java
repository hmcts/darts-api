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

    private static final String EARLIEST_IS_CURRENT_CLEAR_UP_DATE = "2024-12-01";
    private static final OffsetDateTime LEGACY_EVENT_DATE = OffsetDateTime.parse(EARLIEST_IS_CURRENT_CLEAR_UP_DATE + "T00:00:00+00:00").minusDays(1);

    @BeforeEach
    void beforeEach() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(TestUtils.AUTOMATION_USER_ID);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        this.cleanupCurrentFlagEventProcessor = new CleanupCurrentFlagEventProcessorImpl(EARLIEST_IS_CURRENT_CLEAR_UP_DATE, eventRepository, hearingRepository);
    }

    @Test
    void givenEventCleanUpProcessor_whenVersionedEventsAreFound_thenOlderVersionsAreMarkedAsNonCurrentWhenHearingsMatch() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        Map<Integer, List<EventEntity>> eventIdMap =
            transactionalUtil.executeInTransaction(() -> eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false));

        assertAllEventsAreCurrent(eventIdMap);
        eventIdMap.keySet().forEach(eventId -> {
            transactionalUtil.executeInTransaction(() -> cleanupCurrentFlagEventProcessor.processEvent(eventId));
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
        newEventEntity.setCreatedDateTime(EventStub.STARTED_AT);
        eventStub.saveEvent(newEventEntity);
        Map<Integer, List<EventEntity>> eventIdMap =
             eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false, EventStub.STARTED_AT);
        eventIdMap.get(2).add(newEventEntity);
        assertAllEventsAreCurrent(eventIdMap);
        eventIdMap.keySet().forEach(eventId -> {
            transactionalUtil.executeInTransaction(() -> cleanupCurrentFlagEventProcessor.processEvent(eventId));
            assertOnlyOneCurrentPerEventId(eventId, newEventEntity.getId());
        });
        assertOnlyOneCurrentPerEventId(eventIdMap, newEventEntity.getId());
    }

    @Test
    void givenEventCleanUpProcessor_whenVersionedEventsAreFound_thenOlderVersionsAreMarkedAsNonCurrentWhenHearingsMatchNewEventsInsertedOutOfOrder() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        Map<Integer, List<EventEntity>> eventIdMap =
            transactionalUtil.executeInTransaction(() -> eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false));

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
            transactionalUtil.executeInTransaction(() -> cleanupCurrentFlagEventProcessor.processEvent(eventId));
            assertOnlyOneCurrentPerEventId(eventId);
        });
        assertOnlyOneCurrentPerEventId(eventIdMap);
    }

    @Test
    void cleanupCurrentFlagEventProcessor_shouldNotChangeTheIsCurrentFlagForAVersionedEvent_whenTheOriginalEventIsFromALegacyCase() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(3, 3, false, LEGACY_EVENT_DATE);

        assertAllEventsAreCurrent(eventIdMap);

        eventIdMap.keySet().forEach(eventId -> {
            transactionalUtil.executeInTransaction(() -> cleanupCurrentFlagEventProcessor.processEvent(eventId));
            assertAllEventsAreCurrent(eventId);
        });
        assertAllEventsAreCurrent(eventIdMap);
    }

    private void assertOnlyOneCurrentPerEventId(Map<Integer, List<EventEntity>> eventIdMap, Long... eveIdsToExclude) {
        eventIdMap.keySet()
            .forEach(eventId -> assertOnlyOneCurrentPerEventId(eventId, eveIdsToExclude));
    }

    private void assertOnlyOneCurrentPerEventId(Integer eventId, Long... eveIdsToExclude) {
        if (eventId == 0) {
            return;
        }
        List<EventEntity> eventEntities = eventRepository.findAllByEventId(eventId);
        assertOnlyOneCurrentPerEventId(eventEntities, eveIdsToExclude);
    }

    private void assertOnlyOneCurrentPerEventId(List<EventEntity> eventEntities, Long... eveIdsToExclude) {
        List<Long> eventIdsToExclude = List.of(eveIdsToExclude);
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
            assertThat(eventEntity.getLastModifiedById()).isEqualTo(TestUtils.AUTOMATION_USER_ID);
            assertThat(eventEntity.getLastModifiedDateTime())
                .isCloseTo(OffsetDateTime.now(), TestUtils.TIME_TOLERANCE);
        });
    }

    private void assertAllEventsAreCurrent(Integer eventId) {
        assertAllEventsAreCurrent(eventRepository.findAllByEventId(eventId));
    }

    private void assertAllEventsAreCurrent(Map<Integer, List<EventEntity>> eventIdMap) {
        assertAllEventsAreCurrent(eventIdMap.values().stream().flatMap(List::stream).toList());
    }

    private void assertAllEventsAreCurrent(List<EventEntity> eventIdMap) {
        eventIdMap.forEach(eventEntity -> assertTrue(eventEntity.getIsCurrent()));
    }
}