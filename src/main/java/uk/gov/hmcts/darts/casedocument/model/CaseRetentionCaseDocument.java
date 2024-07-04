package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CaseRetentionCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final RetentionPolicyTypeCaseDocument retentionPolicyType;
    private final CaseManagementRetentionCaseDocument caseManagementRetention;
    private final String totalSentence;
    private final OffsetDateTime retainUntil;
    private final OffsetDateTime retainUntilAppliedOn;
    private final String currentState;
    private final String comments;
    private final String retentionObjectId;
    private final Integer submittedBy;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RetentionPolicyTypeCaseDocument extends CreatedModifiedCaseDocument {

        private final Integer id;
        private final String fixedPolicyKey;
        private final String policyName;
        private final String displayName;
        private final String duration;
        private final OffsetDateTime policyStart;
        private final OffsetDateTime policyEnd;
        private final String description;
        private final String retentionPolicyObjectId;
    }

    @Data
    public static class CaseManagementRetentionCaseDocument {

        private final Integer id;
        private final RetentionPolicyTypeCaseDocument retentionPolicyType;
        private final EventCaseDocument event;
        private final String totalSentence;
    }
}
