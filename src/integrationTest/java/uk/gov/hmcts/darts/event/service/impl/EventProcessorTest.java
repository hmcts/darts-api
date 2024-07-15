package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class EventProcessorTest extends PostgresIntegrationBase {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStub eventStub;

    @Autowired
    private HearingStub hearingStub;

    @Autowired
    private AutomatedTaskProcessorFactory eventProcessorFactory;

    @Test
    void testProcess() {
        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        Map<Integer, List<EventEntity>> eventIdMap = eventStub.generateEventIdEventsIncludingZeroEventId(10);

        List<Integer> processedCurrentEventIds = eventProcessorFactory.createEventProcessor(5).processCurrentEvent();
        Assertions.assertEquals(5, processedCurrentEventIds.size());

        // assert that only one of the event ids is set to current
        for (Integer eventId  : processedCurrentEventIds) {
            Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventId)));
            Assertions.assertTrue(eventIdMap.get(eventId).get(1).getIsCurrent());
        }

        // process second batch
        List<Integer> processedCurrentEventIds2 = eventProcessorFactory.createEventProcessor(4).processCurrentEvent();

        Set<Integer> eventIdIntersectionBetweenProcessing = processedCurrentEventIds.stream()
            .distinct()
            .filter(processedCurrentEventIds2::contains)
            .collect(Collectors.toSet());
        Assertions.assertTrue(eventIdIntersectionBetweenProcessing.isEmpty());
        Assertions.assertEquals(4, processedCurrentEventIds2.size());

        // assert that only one of the event ids is set to current
        for (Integer eventId  : processedCurrentEventIds2) {
            Assertions.assertTrue(eventStub.isOnlyOneOfTheEventIdSetToCurrent(eventIdMap.get(eventId)));
            Assertions.assertTrue(eventIdMap.get(eventId).get(1).getIsCurrent());
        }

        // process third batch which is expected to be empty
        Assertions.assertTrue(eventProcessorFactory.createEventProcessor(1).processCurrentEvent().isEmpty());
    }
}