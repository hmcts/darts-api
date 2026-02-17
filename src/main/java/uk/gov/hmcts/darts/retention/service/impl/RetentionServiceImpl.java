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
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionConfidenceCategoryMapperRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.mapper.RetentionMapper;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.time.Duration.between;

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
    @Value("${darts.retention.days-between-events:10}")
    private final Period daysBetweenEvents;

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
        retentionConfidenceCategoryMapperRepository.findByConfidenceCategory(confidenceCategory)
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
    public RetentionConfidenceCategoryEnum getConfidenceCategory(CourtCaseEntity courtCase) {
        RetentionConfidenceCategoryEnum confidenceCategory = null;

        List<EventEntity> eventList = findCurrentEntitiesHelper.getCurrentEvents(courtCase);
        if (CollectionUtils.isNotEmpty(eventList)) {
            eventList.sort(Comparator.comparing(EventEntity::getCreatedDateTime).reversed());
            EventEntity latestEvent = eventList.get(0);
            //find latest closed event
            Optional<EventEntity> latestClosedEvent =
                eventList.stream().filter(eventEntity -> closeEvents.contains(latestEvent.getEventType().getEventName())).findFirst();
            if (latestClosedEvent.isPresent() && latestEvent.getId().equals(latestClosedEvent.get().getId())) {
                // If the latest event in the case is "Case Closed" or "Archive Case" event
                confidenceCategory = RetentionConfidenceCategoryEnum.CASE_CLOSED;
            } else if (latestClosedEvent.isPresent()) {
                confidenceCategory = getRetentionConfidenceCategoryEnumBasedOnDates(latestClosedEvent.get(), latestEvent);
            }
        }
        return confidenceCategory;
    }

    private RetentionConfidenceCategoryEnum getRetentionConfidenceCategoryEnumBasedOnDates(EventEntity latestClosedEvent, EventEntity latestEvent) {
        RetentionConfidenceCategoryEnum confidenceCategory;
        OffsetDateTime closedEventDateTime = latestClosedEvent.getCreatedDateTime();
        OffsetDateTime latestEventDateTime = latestEvent.getCreatedDateTime();
        long daysBetween = between(closedEventDateTime, latestEventDateTime).toDays();
        if (daysBetween <= daysBetweenEvents.getDays()) {
            // if the latest "Case Closed" or "Archive Case" event is NOT the latest non-log event, but the latest non-log event occurs
            // WITHIN 10 days of the "Case Closed" or "Archive Case" event
            confidenceCategory = RetentionConfidenceCategoryEnum.CASE_CLOSED_WITHIN;
        } else {
            // if the latest "Case Closed" or "Archive Case" event is NOT the latest non-log event, but the latest non-log event occurs
            // MORE THAN 10 days after the "Case Closed" or "Archive Case" event
            confidenceCategory = RetentionConfidenceCategoryEnum.MAX_EVENT_OUTWITH;
        }
        return confidenceCategory;
    }

    private RetentionConfidenceCategoryEnum getRetentionConfidenceCategoryForMedia(CourtCaseEntity courtCase) {
        RetentionConfidenceCategoryEnum confidenceCategory;
        //look for the last audio and use its recorded date
        List<MediaEntity> mediaList = findCurrentEntitiesHelper.getCurrentMedia(courtCase);
        if (mediaList.isEmpty()) {
            //look for the last hearing date and use that
            confidenceCategory = RetentionConfidenceCategoryEnum.CASE_CREATION_10271050;//AGED_CASE_MAX_HEARING_CLOSED;
        } else {
            mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
            confidenceCategory = RetentionConfidenceCategoryEnum.MEDIA_LATEST_10261070;//AGED_CASE_MAX_MEDIA_CLOSED;
        }
        return confidenceCategory;
    }
}
