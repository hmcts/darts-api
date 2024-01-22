package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class CaseRetentionStub {
    private final UserAccountRepository userAccountRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Transactional
    public CaseRetentionEntity createCaseRetentionObject(CourtCaseEntity courtCase,
                                                         CaseRetentionStatus retentionStatus, OffsetDateTime retainUntilDate, boolean isManual) {
        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
        caseRetentionEntity.setCourtCase(courtCase);
        caseRetentionEntity.setRetentionPolicyType(retentionPolicyTypeRepository.getReferenceById(1));
        caseRetentionEntity.setTotalSentence("10 years?");
        caseRetentionEntity.setRetainUntil(retainUntilDate);
        caseRetentionEntity.setRetainUntilAppliedOn(currentTimeHelper.currentOffsetDateTime());
        caseRetentionEntity.setCurrentState(retentionStatus.name());
        caseRetentionEntity.setComments("a comment");
        caseRetentionEntity.setManualOverride(isManual);
        caseRetentionEntity.setManualOverride(isManual);
        caseRetentionEntity.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime());
        caseRetentionEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
        caseRetentionEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setSubmittedBy(userAccountRepository.getReferenceById(0));
        caseRetentionRepository.saveAndFlush(caseRetentionEntity);
        return caseRetentionEntity;
    }

}
