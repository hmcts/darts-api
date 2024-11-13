package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplyRetentionProcessorImpl implements ApplyRetentionProcessor {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final CaseRepository caseRepository;

    @Value("${darts.data-management.pending-retention-duration: 7d}")
    private final Duration pendingRetentionDuration;

    @Override
    public void processApplyRetention(Integer batchSize) {
        List<CaseRetentionEntity> caseRetentionEntities =
            caseRetentionRepository.findPendingRetention(currentTimeHelper.currentOffsetDateTime().minus(pendingRetentionDuration),
                                                         Limit.of(batchSize));
        processList(caseRetentionEntities);

    }

    protected void processList(List<CaseRetentionEntity> caseRetentionEntities) {
        Set<Integer> processedCases = new HashSet<>();

        //List is ordered in createdDateTime desc order
        for (CaseRetentionEntity caseRetentionEntity : caseRetentionEntities) {
            CourtCaseEntity courtCaseEntity = caseRetentionEntity.getCourtCase();
            if (processedCases.contains(courtCaseEntity.getId())) {
                caseRetentionEntity.setCurrentState(CaseRetentionStatus.IGNORED.name());
                caseRetentionRepository.save(caseRetentionEntity);
                continue;
            }

            caseRetentionEntity.setRetainUntilAppliedOn(currentTimeHelper.currentOffsetDateTime());
            caseRetentionEntity.setCurrentState(CaseRetentionStatus.COMPLETE.name());

            courtCaseEntity.setRetentionUpdated(true);
            courtCaseEntity.setRetentionRetries(0);

            caseRetentionRepository.save(caseRetentionEntity);
            caseRepository.save(courtCaseEntity);

            processedCases.add(courtCaseEntity.getId());
        }
    }
}
