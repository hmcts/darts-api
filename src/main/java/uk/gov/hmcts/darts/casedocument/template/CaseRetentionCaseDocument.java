package uk.gov.hmcts.darts.casedocument.template;

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
        private Integer id;
        private String fixedPolicyKey;
        private String policyName;
        private String displayName;
        private String duration;
        private OffsetDateTime policyStart;
        private OffsetDateTime policyEnd;
        private String description;
        private String retentionPolicyObjectId;
    }

    @Data
    public static class CaseManagementRetentionCaseDocument {
        private Integer id;
        private RetentionPolicyTypeCaseDocument retentionPolicyType;
        private EventCaseDocument event;
        private String totalSentence;
    }
}
