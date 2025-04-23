package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.PostRetentionResponse;

import java.time.LocalDate;

public interface RetentionPostService {

    PostRetentionResponse postRetention(Boolean validateOnly, PostRetentionRequest postRetentionRequest);

    CaseRetentionEntity createNewCaseRetention(PostRetentionRequest postRetentionRequest,
                                               CourtCaseEntity courtCase,
                                               LocalDate newRetentionDate,
                                               UserAccountEntity userAccount,
                                               CaseRetentionStatus caseRetentionStatus,
                                               RetentionConfidenceCategoryEnum retentionConfidenceCategory);

    CaseRetentionEntity createNewCaseRetention(RetentionPolicyEnum retentionPolicyEnum,
                                               String comments,
                                               CourtCaseEntity courtCase,
                                               LocalDate newRetentionDate,
                                               UserAccountEntity userAccount,
                                               CaseRetentionStatus caseRetentionStatus,
                                               RetentionConfidenceCategoryEnum retentionConfidenceCategory);
}
