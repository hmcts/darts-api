package uk.gov.hmcts.darts.audit.api;

import java.util.Set;

import static java.util.Objects.nonNull;

@FunctionalInterface
public interface AuditActivityProvider {

    Set<AuditActivity> getAuditActivities();

    default boolean notNullAndDifferent(String patchValue, String prePatchedValue) {
        return nonNull(patchValue) && !patchValue.equals(prePatchedValue);
    }

    default boolean notNullAndDifferent(Boolean patchValue, Boolean prePatchedValue) {
        return nonNull(patchValue) && !patchValue.equals(prePatchedValue);
    }

    default boolean notNullAndDifferent(Set<Integer> prePatchValues, Set<Integer> patchValues) {
        return nonNull(patchValues) && !patchValues.equals(prePatchValues);
    }
}
