package uk.gov.hmcts.darts.event.service.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;

@Getter
@Slf4j
@Component
public class CleanupCurrentFlagEventProcessorImpl implements CleanupCurrentFlagEventProcessor {
    private final EventRepository eventRepository;
    private final HearingRepository hearingRepository;
    private final OffsetDateTime earliestIsCurrentClearUpDate;

    public CleanupCurrentFlagEventProcessorImpl(
        @Value("${darts.events.earliest-is-current-clear-up-date}") String earliestIsCurrentClearUpDate,
            EventRepository eventRepository,
            HearingRepository hearingRepository
        ) {
        this.earliestIsCurrentClearUpDate = LocalDate.parse(earliestIsCurrentClearUpDate)
            .atStartOfDay()
            .atOffset(UTC);
        this.eventRepository = eventRepository;
        this.hearingRepository = hearingRepository;
    }

    @Override
    public void processEvent(Integer eventId) {
        if (eventId == null || eventId == 0) {
            return;
        }
        log.info("Cleaning up event id {} so that only one event_id is marked as current per hearing group", eventId);
        List<EventRepository.EventIdAndHearingIds> theLatestCreatedEventPrimaryKeyForTheEventId =
            eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(eventId);

        for (EventRepository.EventIdAndHearingIds eventIdAndHearingIds : theLatestCreatedEventPrimaryKeyForTheEventId) {
            supersedeOldEvents(eventIdAndHearingIds);
        }
    }

    /*
    Mark all but the latest identical event as is_current=false and remove link to hearing.
     */
    private void supersedeOldEvents(EventRepository.EventIdAndHearingIds eventIdAndHearingIds) {
        List<EventEntity> eventsWithSameEventIdAndHearings = getEventsWithSameEventIdAndHearings(eventIdAndHearingIds.getEveId(),
                                                                                                 eventIdAndHearingIds.getEventId());
        List<EventEntity> eventsToBeSuperseded = eventsWithSameEventIdAndHearings.stream()
            .sorted(Comparator.comparing(EventEntity::getCreatedDateTime).reversed())
            .collect(Collectors.toList());
        eventsToBeSuperseded.removeFirst();
        if (CollectionUtils.isNotEmpty(eventsToBeSuperseded)) {
            List<Integer> eveIdsThatHaveBeenSuperseded = new ArrayList<>();
            for (EventEntity eventToBeSuperseded : eventsToBeSuperseded) {
                if (eventToBeSuperseded.getCreatedDateTime().isBefore(earliestIsCurrentClearUpDate)) {
                    log.info("Event version with event id {} received into modernised DARTS is for a legacy event. Skipping event is_current cleanup " +
                                 "for eve_id {}",
                         eventIdAndHearingIds.getEventId(),
                         eventIdAndHearingIds.getEveId());
                    continue;
                }
                eventToBeSuperseded.setIsCurrent(false);
                eventToBeSuperseded.setHearingEntities(new ArrayList<>());
                eveIdsThatHaveBeenSuperseded.add(eventToBeSuperseded.getId());
            }

            eventRepository.saveAllAndFlush(eventsToBeSuperseded);
            log.debug("Updated following events for event id {} excluding primary key {} where hearings match {}, {}",
                      eventIdAndHearingIds.getEventId(),
                      eventIdAndHearingIds.getEveId(),
                      eventIdAndHearingIds.getHearingIds(),
                      eveIdsThatHaveBeenSuperseded);
        }
    }

    private List<EventEntity> getEventsWithSameEventIdAndHearings(Integer eveId, Integer eventId) {
        List<EventEntity> returnList = new ArrayList<>();
        List<Integer> hearingIdsToMatch = hearingRepository.findHearingIdsByEventId(eveId);
        List<EventEntity> eventsWithSameEventId = eventRepository.findAllByEventId(eventId);
        for (EventEntity eventWithSameEventId : eventsWithSameEventId) {
            List<Integer> hearingIdsForMatchingEvent = hearingRepository.findHearingIdsByEventId(eventWithSameEventId.getId());
            if (CollectionUtils.isEqualCollection(hearingIdsToMatch, hearingIdsForMatchingEvent)) {
                returnList.add(eventWithSameEventId);
            }
        }
        return returnList;
    }

}
