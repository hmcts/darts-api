package uk.gov.hmcts.darts.casedocument.template;

import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public class CaseRetentionCaseDocument {

    private Integer id;
    private RetentionPolicyTypeEntity retentionPolicyType;
    private CaseManagementRetentionEntity caseManagementRetention;
    private String totalSentence;
    private OffsetDateTime retainUntil;
    private OffsetDateTime retainUntilAppliedOn;
    private String currentState;
    private String comments;
    private String retentionObjectId;
    private UserAccountEntity submittedBy;
}
