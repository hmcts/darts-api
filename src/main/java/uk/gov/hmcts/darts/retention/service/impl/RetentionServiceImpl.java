package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionServiceImpl implements RetentionService {

    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionConfidenceCategoryMapperRepository retentionConfidenceCategoryMapperRepository;
    private final CaseRepository caseRepository;
    private final RetentionMapper retentionMapper;
    private final Clock clock;

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

}
