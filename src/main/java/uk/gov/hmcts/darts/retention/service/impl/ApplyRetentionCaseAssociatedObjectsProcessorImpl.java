package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyRetentionCaseAssociatedObjectsProcessorImpl implements ApplyRetentionCaseAssociatedObjectsProcessor {

    @Value("${darts.retention.max-retention-retries: 3}")
    int maxRetentionRetries;

    private final CaseRepository caseRepository;
    private final ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl singleCaseProcessor;
    private final UserIdentity userIdentity;

    @Override
    public void processApplyRetentionToCaseAssociatedObjects(Integer batchSize) {

        var casesIds = findCasesNeedingRetentionAppliedToAssociatedObjects(batchSize);

        for (var courtCaseId : casesIds) {
            Optional<CourtCaseEntity> courtCaseOpt = caseRepository.findById(courtCaseId);
            if (courtCaseOpt.isEmpty()) {
                log.error("Case with id '{}' not found", courtCaseId);
                continue;
            }
            CourtCaseEntity courtCase = courtCaseOpt.get();
            courtCase.setRetentionUpdated(false);
            courtCase.setLastModifiedBy(userIdentity.getUserAccount());
            caseRepository.save(courtCase);
            try {
                singleCaseProcessor.processApplyRetentionToCaseAssociatedObjects(courtCase.getId());
            } catch (Exception exc) {
                log.error("Error applying retention to case associated objects for case id '{}'", courtCase.getId(), exc);
                courtCase.setRetentionRetries(courtCase.getRetentionRetries() + 1);
                courtCase.setRetentionUpdated(true);
                courtCase.setLastModifiedBy(userIdentity.getUserAccount());
                caseRepository.save(courtCase);
            }
        }
    }

    private List<Integer> findCasesNeedingRetentionAppliedToAssociatedObjects(Integer batchSize) {
        return caseRepository.findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(maxRetentionRetries, Limit.of(batchSize));
    }

}

