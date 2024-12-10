package uk.gov.hmcts.darts.event.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;

import java.util.List;

@Service
@Slf4j
public class RemoveDuplicateEventsProcessorImpl implements RemoveDuplicateEventsProcessor {

    private static final int CHUNK_SIZE = 1000;
    private final EventRepository eventRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final CaseManagementRetentionRepository caseManagementRetentionRepository;
    private final CaseRetentionRepository caseRetentionRepository;

    public RemoveDuplicateEventsProcessorImpl(
        EventRepository eventRepository,
        CurrentTimeHelper currentTimeHelper,
        CaseManagementRetentionRepository caseManagementRetentionRepository,
        CaseRetentionRepository caseRetentionRepository) {
        this.eventRepository = eventRepository;
        this.currentTimeHelper = currentTimeHelper;
        this.caseManagementRetentionRepository = caseManagementRetentionRepository;
        this.caseRetentionRepository = caseRetentionRepository;
    }

    @Override
    @Transactional
    public void processEvent(Integer eventId) {
        List<EventEntity> eventEntities = eventRepository.findDuplicateEventIds(eventId);
        if (eventEntities.isEmpty() || eventEntities.size() == 1) {
            //No need to continue if there are no duplicates
            return;
        }
        // Keep the first event and delete all future ones
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