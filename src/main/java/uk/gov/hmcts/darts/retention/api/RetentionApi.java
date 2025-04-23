package uk.gov.hmcts.darts.retention.api;

import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;

import java.time.LocalDate;

public interface RetentionApi {
    LocalDate applyPolicyStringToDate(LocalDate dateToAppend, String policyString, RetentionPolicyTypeEntity retentionPolicyType);

    CaseRetentionEntity createRetention(RetentionPolicyEnum retentionPolicyEnum,
                                        String comments,
                                        CourtCaseEntity courtCase,
                                        LocalDate newRetentionDate,
                                        UserAccountEntity userAccount,
                                        CaseRetentionStatus caseRetentionStatus,
                                        RetentionConfidenceCategoryEnum retentionConfidenceCategory);

    CourtCaseEntity updateCourtCaseConfidenceAttributesForRetention(CourtCaseEntity courtCase, RetentionConfidenceCategoryEnum confidenceCategory);
}
