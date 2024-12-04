package uk.gov.hmcts.darts.event.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.Comparator.comparing;

@Service
@Slf4j
public class RemoveDuplicateEventsProcessorImpl implements RemoveDuplicateEventsProcessor {

    private static final int CHUNK_SIZE = 1000;

    private final OffsetDateTime earliestRemovableEventDate;
    private final int clearUpWindow;
    private final EventRepository eventRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final CaseManagementRetentionRepository caseManagementRetentionRepository;
    private final CaseRetentionRepository caseRetentionRepository;

    public RemoveDuplicateEventsProcessorImpl(
        @Value("${darts.events.duplicates.earliest-removable-event-date}") LocalDate earliestRemovableEventDate,
        @Value("${darts.events.duplicates.clear-up-window}") int clearUpWindow,
        EventRepository eventRepository,
        CurrentTimeHelper currentTimeHelper,
        CaseManagementRetentionRepository caseManagementRetentionRepository,
        CaseRetentionRepository caseRetentionRepository) {

        this.earliestRemovableEventDate = earliestRemovableEventDate
            .atStartOfDay()
            .atOffset(UTC);
        this.clearUpWindow = clearUpWindow;
        this.eventRepository = eventRepository;
        this.currentTimeHelper = currentTimeHelper;
        this.caseManagementRetentionRepository = caseManagementRetentionRepository;
        this.caseRetentionRepository = caseRetentionRepository;
    }

    @Override
    @Transactional
    public void processEvent(Integer eventId) {

        OffsetDateTime today = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime startDateTime = today.minusDays(clearUpWindow);
        OffsetDateTime minDate = earliestRemovableEventDate.isAfter(startDateTime) ? earliestRemovableEventDate : startDateTime;
        //TODO confirm if we should use created_ts or timestamp -> if timestamp do we also need to strict by mod start time
        List<EventEntity> eventEntities = eventRepository.findDuplicateEventIds(eventId, minDate);
        if (eventEntities.isEmpty()) {
            //No need to continue if there are no duplicates
            return;
        }
        eventEntities.sort(comparing(EventEntity::getCreatedDateTime));
        // Keep the last (latest) event and delete the rest
        List<EventEntity> eventEntitiesToDelete = eventEntities.subList(1, eventEntities.size());


        List<Integer> caseManagementIdsToBeDeleted = caseManagementRetentionRepository.getIdsForEvents(eventEntitiesToDelete);
        caseRetentionRepository.deleteAllByCaseManagementIdsIn(caseManagementIdsToBeDeleted);
        caseRetentionRepository.flush();
        caseManagementRetentionRepository.deleteAllByEventEntityIn(eventEntitiesToDelete);
        caseManagementRetentionRepository.flush();
        eventRepository.deleteAll(eventEntitiesToDelete);
        eventRepository.flush();

        log.info("Duplicate events found. Removing events with the following event_id:message_id combination {}",
                 eventEntitiesToDelete.stream().map(e -> e.getEventId() + ":" + e.getMessageId()).toList());
    }
}