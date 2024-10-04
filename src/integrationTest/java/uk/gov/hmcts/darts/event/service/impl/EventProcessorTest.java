package uk.gov.hmcts.darts.event.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

class EventProcessorTest extends PostgresIntegrationBase {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStub eventStub;

    @Autowired
    private HearingStub hearingStub;

    @Autowired
    private AutomatedTaskProcessorFactory eventProcessorFactory;
    @MockBean
    private UserIdentity userIdentity;

    @Test
    void testProcess() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(TestUtils.AUTOMATION_USER_ID);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(10);

        List<Integer> processedCurrentEventIds = eventProcessorFactory.createCleanupCurrentFlagEventProcessor(5).processCurrentEvent();
        Assertions.assertEquals(5, processedCurrentEventIds.size());
        Assertions.assertEquals(5, processedCurrentEventIds.size());
        assertAllEventsThatShouldRemainUntounchedAreUntouched(eventIdMap, processedCurrentEventIds);
        assertThatWeOnlyProcessEventIdsWhichCorrespondToMoreThanOneEvent(eventIdMap, processedCurrentEventIds);

        // assert that only one of the event ids is set to current
        for (Integer eventId : processedCurrentEventIds) {
            Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventId)));
            Assertions.assertTrue(eventIdMap.get(eventId).get(1).getIsCurrent());
        }

        // process second batch
        List<Integer> processedCurrentEventIds2 = eventProcessorFactory.createCleanupCurrentFlagEventProcessor(4).processCurrentEvent();
        Assertions.assertEquals(4, processedCurrentEventIds2.size());
        assertAllEventsThatShouldRemainUntounchedAreUntouched(eventIdMap, processedCurrentEventIds2);
        assertThatWeOnlyProcessEventIdsWhichCorrespondToMoreThanOneEvent(eventIdMap, processedCurrentEventIds2);

        // assert that only one of the event ids is set to current
        for (Integer eventId : processedCurrentEventIds2) {
            Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventId)));
            Assertions.assertTrue(eventIdMap.get(eventId).get(1).getIsCurrent());
        }

        // assert that only one of the event ids has hearings
        for (Integer eventId : processedCurrentEventIds2) {
            Assertions.assertTrue(eventStub.hasOnlyOneOfTheEventIdsGotHearings(eventIdMap.get(eventId)));
        }

        // process third batch which is expected to be empty
        Assertions.assertTrue(eventProcessorFactory
                                  .createCleanupCurrentFlagEventProcessor(1).processCurrentEvent().isEmpty());
    }

    private void assertAllEventsThatShouldRemainUntounchedAreUntouched(Map<Integer, List<EventEntity>> generatedEventIds,
                                                                       List<Integer> processedCurrentEventIds) {

        // gets the diff i.e. what should not have been processed
        Collection<Integer> eventsThatShouldNotBeProcessed
            = CollectionUtils.disjunction(generatedEventIds.keySet().stream().toList(), processedCurrentEventIds);

        // assert that each event mapped to an event id that should NOT be processed remains untouched
        eventsThatShouldNotBeProcessed.forEach(event ->
                                                   generatedEventIds.get(event).forEach(eventEntity ->
                                                                                            // assert that the current value is still the current value
                                                                                            Assertions.assertTrue(eventEntity.getIsCurrent())));
    }

    private void assertThatWeOnlyProcessEventIdsWhichCorrespondToMoreThanOneEvent(Map<Integer,
        List<EventEntity>> generatedEventIds, List<Integer> processedCurrentEventIds) {
        processedCurrentEventIds.forEach(eventId -> Assertions.assertTrue(generatedEventIds.get(eventId).size() > 1));
    }
}