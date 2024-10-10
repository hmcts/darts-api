package uk.gov.hmcts.darts.event.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Slf4j
public class CleanupCurrentFlagEventProcessorImpl implements CleanupCurrentFlagEventProcessor {
    private final Integer batchSize;
    private final EventRepository eventRepository;
    private final HearingRepository hearingRepository;
    private final UserIdentity userIdentity;

    @Override
    public List<Integer> processCurrentEvent() {
        List<Integer> processedEventIdLst = new ArrayList<>();
        log.debug("Batch size to process event ids for {}", batchSize);
        List<Integer> eventEntityReturned = eventRepository.findCurrentEventIdsWithDuplicates(batchSize);
        if (log.isDebugEnabled()) {
            log.debug("Event ids being processed {}", eventEntityReturned
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        }

        UserAccountEntity userAccount = userIdentity.getUserAccount();
        for (Integer event : eventEntityReturned) {
            log.debug("Processing event id {}", event);
            List<EventRepository.EventIdAndHearingIds> theLatestCreatedEventPrimaryKeyForTheEventId =
                eventRepository.getTheLatestCreatedEventPrimaryKeyForTheEventId(event);
            for (EventRepository.EventIdAndHearingIds eventIdAndHearingIds : theLatestCreatedEventPrimaryKeyForTheEventId) {
                supersedeOldEvents(eventIdAndHearingIds);

                processedEventIdLst.add(event);
            }
        }
        return processedEventIdLst;
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