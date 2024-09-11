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
        List<Integer> eventEntityReturned = eventRepository.getCurrentEventIdsToBeProcessed(batchSize);
        if (log.isDebugEnabled()) {
            log.debug("Event ids being processed {}", eventEntityReturned
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        }

        eventEntityReturned.forEach(event -> {
            EventRepository.EventIdAndHearingIds eventIdAndHearingIds = eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(event);
            eventRepository.updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
                eventIdAndHearingIds.getEveId(), eventIdAndHearingIds.getEventId(), eventIdAndHearingIds.getHearingIds());

            log.debug("Updated all events for event id {} excluding primary key {} where hearings match {}",
                      eventIdAndHearingIds.getEventId(),
                      eventIdAndHearingIds.getEveId(),
                      eventIdAndHearingIds.getHearingIds());
            processedEventIdLst.add(event);
        });
        return processedEventIdLst;
    }
}