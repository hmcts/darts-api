package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Deprecated
public class CaseRetentionStub {
    private final UserAccountRepository userAccountRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    @Transactional
    public CaseRetentionEntity createCaseRetentionObject(CourtCaseEntity courtCase,
                                                         CaseRetentionStatus retentionStatus, OffsetDateTime retainUntilDate, boolean isManual) {
        RetentionPolicyTypeEntity policy;
        if (isManual) {
            policy = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
                RetentionPolicyEnum.MANUAL.getPolicyKey(),
                currentTimeHelper.currentOffsetDateTime()
            ).getFirst();
        } else {
            policy = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
                RetentionPolicyEnum.DEFAULT.getPolicyKey(),
                currentTimeHelper.currentOffsetDateTime()
            ).getFirst();
        }
        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
        caseRetentionEntity.setCourtCase(courtCase);
        caseRetentionEntity.setRetentionPolicyType(policy);
        caseRetentionEntity.setTotalSentence("10y0m0d");
        caseRetentionEntity.setRetainUntil(retainUntilDate);
        caseRetentionEntity.setRetainUntilAppliedOn(currentTimeHelper.currentOffsetDateTime());
        caseRetentionEntity.setCurrentState(retentionStatus.name());
        caseRetentionEntity.setComments("a comment");
        caseRetentionEntity.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime());
        caseRetentionEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
        caseRetentionEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setSubmittedBy(userAccountRepository.getReferenceById(0));
        dartsDatabaseSaveStub.save(caseRetentionEntity);
        return caseRetentionEntity;
    }

    @Transactional
    public CaseRetentionEntity createCaseRetentionObject(CourtCaseEntity courtCase, OffsetDateTime retainUntilDate) {
        return createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, retainUntilDate, false);
    }

}