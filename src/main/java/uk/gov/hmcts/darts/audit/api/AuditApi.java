package uk.gov.hmcts.darts.audit.api;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface AuditApi {


    default void record(AuditActivity activity, UserAccountEntity userAccountEntity) {
        record(activity, userAccountEntity, null, null);
    }

    default void record(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase) {
        record(activity, userAccountEntity, courtCase, null);
    }

    default void record(AuditActivity activity, UserAccountEntity userAccountEntity, String additionalData) {
        record(activity, userAccountEntity, null, additionalData);
    }

    void record(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase, String additionalData);

    void record(AuditActivity activity);

    void recordAll(AuditActivityProvider auditActivityProvider);

    void recordAll(AuditActivityProvider auditActivityProvider, CourtCaseEntity courtCase);
}
