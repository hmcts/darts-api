package uk.gov.hmcts.darts.retention.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;
import uk.gov.hmcts.darts.retention.service.RetentionPostService;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class RetentionApiImpl implements RetentionApi {

    private final RetentionDateHelper retentionDateHelper;
    private final RetentionPostService retentionPostService;

    @Override
    public LocalDate applyPolicyStringToDate(LocalDate dateToAppend, String policyString, RetentionPolicyTypeEntity retentionPolicyType) {
        return retentionDateHelper.applyPolicyString(dateToAppend, policyString, retentionPolicyType);
    }

    @Override
    public CaseRetentionEntity createRetention(PostRetentionRequest postRetentionRequest,
                                               CourtCaseEntity courtCase,
                                               LocalDate newRetentionDate,
                                               UserAccountEntity userAccount,
                                               CaseRetentionStatus caseRetentionStatus,
                                               RetentionConfidenceCategoryEnum retentionConfidenceCategory) {
        return retentionPostService.createNewCaseRetention(postRetentionRequest,
                                                           courtCase,
                                                           newRetentionDate,
                                                           userAccount,
                                                           caseRetentionStatus,
                                                           retentionConfidenceCategory);
    }
}
