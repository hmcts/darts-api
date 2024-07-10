package uk.gov.hmcts.darts.event.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.service.RemoveDuplicateEventsProcessor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Function;

import static java.time.ZoneOffset.UTC;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

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
    public void processRemoveDuplicateEvents() {
        var today = currentTimeHelper.currentLocalDate();
        var startDateTime = today.minusDays(clearUpWindow).atStartOfDay().atOffset(UTC);
        if (startDateTime.isBefore(earliestRemovableEventDate)) {
            startDateTime = earliestRemovableEventDate;
        }
        var endDateTime = today.atTime(23, 59).atOffset(UTC);
        var duplicateEvents = new ArrayList<EventEntity>();

        eventRepository.findAllForDuplicateProcessing(startDateTime, endDateTime).stream()
            .collect(groupingBy(duplicateIdentification()))
            .forEach((key, eventList) -> {
                eventList.sort(comparing(EventEntity::getTimestamp));
                // Keep the last (latest) event and delete the rest
                duplicateEvents.addAll(eventList.subList(0, eventList.size() - 1));
            });

        if (!duplicateEvents.isEmpty()) {
            log.info("duplicate events found: {}", duplicateEvents.stream().map(EventEntity::getId).toList());
            Spliterator<EventEntity> spliterator = duplicateEvents.spliterator();
            List<EventEntity> chunk;

            while (spliterator.estimateSize() > 0) {
                chunk = stream(spliterator, false)
                    .limit(CHUNK_SIZE)
                    .collect(toList());

                if (!chunk.isEmpty()) {
                    var caseManagementIdsToBeDeleted = caseManagementRetentionRepository.getIdsForEvents(chunk);
                    caseRetentionRepository.deleteAllByCaseManagementIdsIn(caseManagementIdsToBeDeleted);
                    caseRetentionRepository.flush();
                    caseManagementRetentionRepository.deleteAllByEventEntityIn(chunk);
                    caseManagementRetentionRepository.flush();
                    eventRepository.deleteAllInBatch(chunk);
                    eventRepository.flush();
                }
            }
        }
    }

    private static Function<EventEntity, String> duplicateIdentification() {
        return e -> e.getEventId() + "-" + e.getMessageId() + "-" + e.getEventText();
    }
}