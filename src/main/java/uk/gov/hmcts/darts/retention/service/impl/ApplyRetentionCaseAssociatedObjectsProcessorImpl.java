package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyRetentionCaseAssociatedObjectsProcessorImpl implements ApplyRetentionCaseAssociatedObjectsProcessor {

    @Value("${darts.retention.max-retention-retries: 3}")
    int maxRetentionRetries;

    private final CaseRepository caseRepository;
    private final ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl singleCaseProcessor;

    @Override
    public void processApplyRetentionToCaseAssociatedObjects() {

        var cases = findCasesNeedingRetentionAppliedToAssociatedObjects();

        for (var courtCase : cases) {
            courtCase.setRetentionUpdated(false);
            caseRepository.save(courtCase);
            try {
                singleCaseProcessor.processApplyRetentionToCaseAssociatedObjects(courtCase.getId());
            } catch (Exception exc) {
                log.error("Error applying retention to case associated objects for case id '{}'", courtCase.getId(), exc);
                courtCase.setRetentionRetries(courtCase.getRetentionRetries() + 1);
                courtCase.setRetentionUpdated(true);
                caseRepository.save(courtCase);
            }
        }
    }

    public List<CourtCaseEntity> findCasesNeedingRetentionAppliedToAssociatedObjects() {
        return caseRepository.findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(maxRetentionRetries);
    }

}

