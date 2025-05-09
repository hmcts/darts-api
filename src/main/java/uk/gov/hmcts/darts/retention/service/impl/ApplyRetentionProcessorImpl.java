package uk.gov.hmcts.darts.retention.service.impl;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplyRetentionProcessorImpl implements ApplyRetentionProcessor {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final ApplyRetentionCaseProcessor applyRetentionCaseProcessor;

    @Value("${darts.data-management.pending-retention-duration: 7d}")
    private final Duration pendingRetentionDuration;

    @Override
    public void processApplyRetention(Integer batchSize) {
        List<Integer> caseRetentionEntitiesIds =
            caseRetentionRepository.findPendingRetention(currentTimeHelper.currentOffsetDateTime().minus(pendingRetentionDuration),
                                                         Limit.of(batchSize));
        log.info("Processing {} case retention entities out of a batch size {}", caseRetentionEntitiesIds.size(), batchSize);
        Set<Integer> processedCases = new HashSet<>();

        //List is ordered in createdDateTime desc order
        for (Integer caseRetentionEntitiesId : caseRetentionEntitiesIds) {
            applyRetentionCaseProcessor.process(processedCases, caseRetentionEntitiesId);
        }

    }

    @Component
    @AllArgsConstructor
    public static class ApplyRetentionCaseProcessor {

        private final CaseRetentionRepository caseRetentionRepository;
        private final CurrentTimeHelper currentTimeHelper;
        private final CaseRepository caseRepository;

        @Transactional
        public void process(Set<Integer> processedCases, int caseRetentionEntitiesId) {
            Optional<CaseRetentionEntity> caseRetentionEntityOpt = caseRetentionRepository.findById(caseRetentionEntitiesId);
            if (caseRetentionEntityOpt.isEmpty()) {
                log.error("CaseRetentionEntity with id {} not found", caseRetentionEntitiesId);
                return;
            }
            CaseRetentionEntity caseRetentionEntity = caseRetentionEntityOpt.get();
            CourtCaseEntity courtCaseEntity = caseRetentionEntity.getCourtCase();
            if (processedCases.contains(courtCaseEntity.getId())) {
                caseRetentionEntity.setCurrentState(CaseRetentionStatus.IGNORED.name());
                caseRetentionRepository.save(caseRetentionEntity);
                return;
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
