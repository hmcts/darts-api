package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplyRetentionProcessorImpl implements ApplyRetentionProcessor {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final CaseRepository caseRepository;


    @Value("${darts.data-management.pending-retention-days: 7}")
    long pendingRetentionDays;

    @Override
    public void processApplyRetention() {
        List<CaseRetentionEntity> caseRetentionEntities =
                caseRetentionRepository.findPendingRetention(currentTimeHelper.currentOffsetDateTime().minusDays(pendingRetentionDays));
        processList(caseRetentionEntities);

    }

    protected void processList(List<CaseRetentionEntity> caseRetentionEntities) {
        List<Integer> processedCases = new ArrayList<>();

        //List is ordered in createdDateTime desc order
        for (CaseRetentionEntity caseRetentionEntity: caseRetentionEntities) {
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
