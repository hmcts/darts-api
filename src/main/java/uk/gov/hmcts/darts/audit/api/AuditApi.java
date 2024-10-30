package uk.gov.hmcts.darts.audit.api;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Optional;

public interface AuditApi {

    default void record(AuditActivity activity, UserAccountEntity userAccountEntity, String additionalData) {
        record(activity, userAccountEntity, Optional.empty(), Optional.ofNullable(additionalData));
    }

    default void record(AuditActivity activity, UserAccountEntity userAccountEntity) {
        record(activity, userAccountEntity, Optional.empty(), Optional.empty());
    }

    default void record(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase) {
        record(activity, userAccountEntity, Optional.ofNullable(courtCase), Optional.empty());
    }

    void record(AuditActivity activity, UserAccountEntity userAccountEntity, Optional<CourtCaseEntity> courtCase, Optional<String> additionalData);

    void record(AuditActivity activity);

    void record(AuditActivity activity, String additionalData);

    void recordAll(AuditActivityProvider auditActivityProvider);

    void recordAll(AuditActivityProvider auditActivityProvider, CourtCaseEntity courtCase);
}