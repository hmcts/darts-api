package uk.gov.hmcts.darts.event.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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

    @Override
    public List<Integer> processCurrentEvent() {
        List<Integer> processedEventIdLst = new ArrayList<>();
        log.info("Batch size to process event ids for {}", batchSize);
        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(batchSize));
        log.info("Event ids being processed{}",  processedEventIdLst.stream().map(Object::toString)
            .collect(Collectors.joining(",")));
        log.info("Number of Event ids to be processed {}",  processedEventIdLst.size());

        eventEntityReturned.forEach(event -> {
            Integer eventIdPrimaryKey = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(event);
            log.info("Current event primary key is {}", eventIdPrimaryKey);
            eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventIdPrimaryKey, event);
            log.info("Updated all events for event id {} excluding primary key {}", event, eventIdPrimaryKey);

            processedEventIdLst.add(event);
        });

        return processedEventIdLst;
    }
}