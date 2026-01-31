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
import uk.gov.hmcts.darts.common.entity.HearingEntity;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private List<String> closeEvents;

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

    public RetentionConfidenceCategoryEnum getConfidenceCategory(CourtCaseEntity courtCase) {
        RetentionConfidenceCategoryEnum confidenceCategory;
        List<EventEntity> eventList = findCurrentEntitiesHelper.getCurrentEvents(courtCase);
        if (CollectionUtils.isNotEmpty(eventList)) {
            eventList.sort(Comparator.comparing(EventEntity::getCreatedDateTime).reversed());
            //find latest closed event
            Optional<EventEntity> closedEvent =
                eventList.stream().filter(eventEntity -> closeEvents.contains(eventEntity.getEventType().getEventName())).findFirst();

            if (closedEvent.isPresent()) {
                confidenceCategory = RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CLOSED;
            } else {
                //look for the last event and use that date
                confidenceCategory = RetentionConfidenceCategoryEnum.AGED_CASE_MAX_EVENT_CLOSED;
            }
        } else if (courtCase.getHearings().isEmpty()) {
            //set to created date
            confidenceCategory = RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CREATION_CLOSED;
        } else {
            //look for the last audio and use its recorded date
            List<MediaEntity> mediaList = findCurrentEntitiesHelper.getCurrentMedia(courtCase);
            if (mediaList.isEmpty()) {
                //look for the last hearing date and use that
                courtCase.getHearings().sort(Comparator.comparing(HearingEntity::getHearingDate).reversed());
                HearingEntity lastHearingEntity = courtCase.getHearings().getFirst();
                confidenceCategory = RetentionConfidenceCategoryEnum.AGED_CASE_MAX_HEARING_CLOSED;
            } else {
                mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
                confidenceCategory = RetentionConfidenceCategoryEnum.AGED_CASE_MAX_MEDIA_CLOSED;
            }
        }
        return confidenceCategory;
    }
}
