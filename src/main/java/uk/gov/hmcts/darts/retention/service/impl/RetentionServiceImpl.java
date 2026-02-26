package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.helper.FindCurrentEntitiesHelper;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionConfidenceCategoryMapperRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.mapper.RetentionMapper;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.time.Duration.between;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RetentionServiceImpl implements RetentionService {

    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionConfidenceCategoryMapperRepository retentionConfidenceCategoryMapperRepository;
    private final CaseRepository caseRepository;
    private final RetentionMapper retentionMapper;
    private final Clock clock;
    private final FindCurrentEntitiesHelper findCurrentEntitiesHelper;
    @Value("#{'${darts.retention.close-events}'.split(',')}")
    private final List<String> closeEvents;

    @Override
    public List<GetCaseRetentionsResponse> getCaseRetentions(Integer caseId) {
        List<CaseRetentionEntity> caseRetentionEntities =
            caseRetentionRepository.findByCaseId(caseId);

        List<GetCaseRetentionsResponse> caseRetentions = new ArrayList<>();
        for (CaseRetentionEntity caseRetentionEntity : caseRetentionEntities) {
            caseRetentions.add(retentionMapper.mapToCaseRetention(caseRetentionEntity));
        }
        return caseRetentions;
    }

    @Override
    public CourtCaseEntity updateCourtCaseConfidenceAttributesForRetention(CourtCaseEntity courtCase,
                                                                           RetentionConfidenceCategoryEnum confidenceCategory) {
        Integer confidenceCategoryId = nonNull(confidenceCategory) ? confidenceCategory.getId() : null;
        retentionConfidenceCategoryMapperRepository.findByConfidenceCategory(confidenceCategoryId)
            .ifPresentOrElse(categoryMapperEntity -> {
                courtCase.setRetConfScore(categoryMapperEntity.getConfidenceScore());
                courtCase.setRetConfReason(categoryMapperEntity.getConfidenceReason());
            }, () -> {
                courtCase.setRetConfScore(null);
                courtCase.setRetConfReason(null);
            });

        courtCase.setRetConfUpdatedTs(OffsetDateTime.now(clock));
        return caseRepository.save(courtCase);
    }

    @Override
    public Integer getConfidenceCategory(CourtCaseEntity courtCase, Duration pendingRetentionDuration,
                                         CaseRetentionEntity caseRetention) {
        Integer confidenceCategory = null;

        List<EventEntity> eventList = findCurrentEntitiesHelper.getCurrentEvents(courtCase);
        if (CollectionUtils.isNotEmpty(eventList)) {
            eventList.sort(Comparator.comparing(EventEntity::getTimestamp).reversed());
            EventEntity latestEvent = eventList.get(0);
            //find latest closed event
            Optional<EventEntity> latestClosedEvent =
                eventList.stream().filter(eventEntity -> closeEvents.contains(eventEntity.getEventType().getEventName())).findFirst();

            if (latestClosedEvent.isPresent()) {
                if (latestEvent.getId().equals(latestClosedEvent.get().getId())) {
                    // If the latest event in the case is "Case Closed" or "Archive Case" event
                    confidenceCategory = caseRetention.getConfidenceCategory();
                    log.info("Latest event is a close event, setting confidence category to case retention confidence category: {} for case id: {}",
                             confidenceCategory, courtCase.getId());
                } else {
                    confidenceCategory = getRetentionConfidenceCategoryEnumBasedOnDates(eventList, latestClosedEvent.get(), pendingRetentionDuration,
                                                                                        caseRetention);
                }
            } else {
                confidenceCategory = caseRetention.getConfidenceCategory();
                log.info("No close events found, setting confidence category to case retention confidence category: {} for case id: {}",
                         confidenceCategory, courtCase.getId());
            }
        }
        return confidenceCategory;
    }

    private Integer getRetentionConfidenceCategoryEnumBasedOnDates(List<EventEntity> eventList,
                                                                   EventEntity latestClosedEvent, Duration pendingRetentionDuration,
                                                                   CaseRetentionEntity caseRetention) {
        Optional<EventEntity> latestNonLogEvent =
            eventList.stream().filter(eventEntity -> !eventEntity.isLogEntry()).findFirst();

        if (latestNonLogEvent.isEmpty()) {
            // if there are no non-log events, then we will categorise based on the closed event;
            log.info("No non-log events found, setting confidence category to case retention confidence category: {} for case id: {}",
                     caseRetention.getConfidenceCategory(), caseRetention.getCourtCase().getId());
            return caseRetention.getConfidenceCategory();
        }
        OffsetDateTime nonLogEventDateTime = latestNonLogEvent.get().getTimestamp();
        OffsetDateTime latestClosedEventDateTime = latestClosedEvent.getTimestamp();
        long daysBetween = between(latestClosedEventDateTime, nonLogEventDateTime).toDays();

        if (daysBetween <= pendingRetentionDuration.toDays()) {
            // if the latest non-log event occurs WITHIN 10 days of the "Case Closed" or "Archive Case" event
            log.info("Latest non-log event occurs within {} duration of close event, setting confidence category to CASE_CLOSED_WITHIN for case id: {}",
                     pendingRetentionDuration, caseRetention.getCourtCase().getId());
            return RetentionConfidenceCategoryEnum.CASE_CLOSED_WITHIN.getId();
        } else {
            // if the latest "Case Closed" or "Archive Case" event is NOT the latest non-log event, but the latest non-log event occurs
            // MORE THAN 10 days after the "Case Closed" or "Archive Case" event
            log.info("Latest non-log event occurs outside retention duration of close event, setting confidence category to MAX_EVENT_OUTWITH for case id: {}",
                     caseRetention.getCourtCase().getId());
            return RetentionConfidenceCategoryEnum.MAX_EVENT_OUTWITH.getId();
        }
    }


}
