package uk.gov.hmcts.darts.event.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.EventProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Slf4j
public class EventProcessorImpl implements EventProcessor {
    private final Integer batchSize;
    private final EventRepository eventRepository;
    private final CurrentTimeHelper currentOffsetDateTime;

    @Override
    public List<Integer> processCurrentEvent() {
        List<Integer> currentEventLst = new ArrayList<>();
        log.info("Batch size to process event ids for {}", batchSize);
        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(batchSize);
        log.info("Event ids being processed{}",  currentEventLst.stream().map(Object::toString)
            .collect(Collectors.joining(",")));
        log.info("Number of Event ids to be processed {}",  currentEventLst.size());

        eventEntityReturned.stream().forEach(event -> {
            Integer eventIdPrimaryKey = eventRepository.getTheCurrentEventPrimaryKeyForEventId(event);
            log.info("Current event primary key is {}", eventIdPrimaryKey);
            eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventIdPrimaryKey, event);
            log.info("Updated all events for event id {} excluding primary key {}", event, eventIdPrimaryKey);

            currentEventLst.add(event);
        });

        return currentEventLst;
    }
}