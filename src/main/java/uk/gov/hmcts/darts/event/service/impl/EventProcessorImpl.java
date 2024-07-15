package uk.gov.hmcts.darts.event.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.EventProcessor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class EventProcessorImpl implements EventProcessor {
    private final Integer batchSize;
    private final EventRepository eventRepository;
    private final CurrentTimeHelper currentOffsetDateTime;

    @Override
    public List<Integer> processCurrentEvent() {
        List<Integer> currentEventLst = new ArrayList<>();
        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(batchSize);
        eventEntityReturned.stream().forEach(event -> {
            Integer eventId = eventRepository.getTheCurrentEventPrimaryKeyForEventId(event);
            eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventId, event);
            currentEventLst.add(event);
        });

        return currentEventLst;
    }
}