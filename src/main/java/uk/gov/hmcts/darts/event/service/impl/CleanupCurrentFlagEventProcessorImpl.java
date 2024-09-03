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
        if (log.isDebugEnabled()) {
            log.debug("Event ids being processed {}", eventEntityReturned
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
            log.debug("Number of Event ids to be processed {}", eventEntityReturned.size());
        }

        eventEntityReturned.forEach(event -> {
            EventRepository.EventIdAndHearingIds eventIdAndHearingIds = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(event);
            log.debug("Current event primary key is {}", eventIdAndHearingIds);
            eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
                eventIdAndHearingIds.getEveId(), eventIdAndHearingIds.getEventId(), eventIdAndHearingIds.getHearingIds());

            log.debug("Updated all events for event id {} excluding primary key {}",
                      eventIdAndHearingIds.getEventId(),
                      eventIdAndHearingIds.getEveId());
            processedEventIdLst.add(event);
        });
        return processedEventIdLst;
    }
}