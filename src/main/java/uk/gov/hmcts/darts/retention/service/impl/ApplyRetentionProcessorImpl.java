package uk.gov.hmcts.darts.retention.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class ApplyRetentionProcessorImpl implements ApplyRetentionProcessor {

    CaseRetentionRepository caseRetentionRepository;

    @Override
    public void processApplyRetention() {
        List<CaseRetentionEntity> caseRetentionEntities =
                caseRetentionRepository.findPendingRetention(OffsetDateTime.now().minusDays(7));
        processList(caseRetentionEntities);

    }

    protected void processList(List<CaseRetentionEntity> caseRetentionEntities) {
        List<Integer> processedCases = new ArrayList<>();
        for (CaseRetentionEntity caseRetentionEntity: caseRetentionEntities) {
            if (processedCases.contains(caseRetentionEntity.getId())) {
                continue;
            }
            //check if there are other records with the same caseId
            Optional<CaseRetentionEntity> latestCaseRetention;
            latestCaseRetention = caseRetentionEntities.stream().filter(caseRetentionEntity1 ->
                    Objects.equals(
                            caseRetentionEntity1.getCourtCase().getId(),
                            caseRetentionEntity.getCourtCase().getId()))
                    .max(Comparator.comparing(CaseRetentionEntity::getCreatedDateTime));
            //find the most recent
            if (latestCaseRetention.isPresent()) {
                latestCaseRetention.get().setRetainUntilAppliedOn(OffsetDateTime.now());
                latestCaseRetention.get().setCurrentState(CaseRetentionStatus.COMPLETE.name());
                caseRetentionRepository.save(latestCaseRetention.get());
                processedCases.add(latestCaseRetention.get().getId());
            }
        }
    }
}
