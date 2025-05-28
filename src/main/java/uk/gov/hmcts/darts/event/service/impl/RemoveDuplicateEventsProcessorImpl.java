package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RemoveDuplicateEventsProcessorImpl implements RemoveDuplicateEventsProcessor {
    private final EventRepository eventRepository;
    private final EventLinkedCaseRepository eventLinkedCaseRepository;
    private final CaseManagementRetentionRepository caseManagementRetentionRepository;
    private final CaseRetentionRepository caseRetentionRepository;

    @Override
    @Transactional
    public boolean findAndRemoveDuplicateEvent(DartsEvent event) {
        List<Long> eventEntitiesIds = eventRepository.findDuplicateEventIds(
            NumberUtils.createInteger(event.getEventId()),
            event.getMessageId(),
            event.getEventText()
        );

        if (eventEntitiesIds.isEmpty() || eventEntitiesIds.size() == 1) {
            //No need to continue if there are no duplicates
            return false;
        }
        // Keep the first event and delete all future ones
        List<Long> eventEntitiesIdsToDelete = eventEntitiesIds.subList(1, eventEntitiesIds.size());

        List<Integer> caseManagementIdsToBeDeleted = caseManagementRetentionRepository.getIdsForEvents(eventEntitiesIdsToDelete);
        caseRetentionRepository.deleteAllByCaseManagementIdsIn(caseManagementIdsToBeDeleted);
        caseRetentionRepository.flush();
        caseManagementRetentionRepository.deleteAllByEventEntityIn(eventEntitiesIdsToDelete);
        caseManagementRetentionRepository.flush();
        eventLinkedCaseRepository.deleteAllByEventIn(eventEntitiesIdsToDelete);
        eventLinkedCaseRepository.flush();
        eventRepository.deleteAllAssociatedHearings(eventEntitiesIdsToDelete);
        eventRepository.deleteAllById(eventEntitiesIdsToDelete);
        eventRepository.flush();

        log.info("Duplicate events found for event_id='{}', message_id='{}', event_text='{}'. Removing the following events {}",
                 event.getEventId(), event.getMessageId(), event.getEventText(), eventEntitiesIdsToDelete);
        return true;
    }
}