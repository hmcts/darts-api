package uk.gov.hmcts.darts.retention.api;

import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.LocalDate;

public interface RetentionApi {
    LocalDate applyPolicyStringToDate(LocalDate dateToAppend, String policyString, RetentionPolicyTypeEntity retentionPolicyType);
}
