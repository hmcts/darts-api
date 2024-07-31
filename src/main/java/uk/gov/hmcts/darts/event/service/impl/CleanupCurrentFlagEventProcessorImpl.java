package uk.gov.hmcts.darts.event.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Slf4j
public class CleanupCurrentFlagEventProcessorImpl implements CleanupCurrentFlagEventProcessor {
    private final Integer batchSize;
    private final EventRepository eventRepository;

    @Override
    public List<Integer> processCurrentEvent() {
        List<Integer> processedEventIdLst = new ArrayList<>();
        log.debug("Batch size to process event ids for {}", batchSize);
        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(Pageable.ofSize(batchSize));
        log.debug("Event ids being processed{}",  processedEventIdLst.stream().map(Object::toString)
            .collect(Collectors.joining(",")));
        log.debug("Number of Event ids to be processed {}",  processedEventIdLst.size());

        List<Integer> eventPrimaryKeysLst = new ArrayList<>();
        List<Integer> eventIdsLst = new ArrayList<>();

        eventEntityReturned.forEach(event -> {
            Integer eventIdPrimaryKey = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(event);
            log.debug("Current event primary key is {}", eventIdPrimaryKey);

            eventPrimaryKeysLst.add(eventIdPrimaryKey);
            eventIdsLst.add(event);

            processedEventIdLst.add(event);
        });

        eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(eventPrimaryKeysLst, eventIdsLst);
        log.debug("Updated all events for event id {} excluding primary key {}", eventIdsLst.stream().map(Object::toString),
                 eventPrimaryKeysLst.stream().map(Object::toString));

        return processedEventIdLst;
    }
}