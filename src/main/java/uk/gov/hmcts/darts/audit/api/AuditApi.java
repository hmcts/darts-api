package uk.gov.hmcts.darts.audit.api;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface AuditApi {

    void record(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase);

    void record(AuditActivity activity);

    void record(AuditActivity activity, String additionalData);

    void recordAll(AuditActivityProvider auditActivityProvider);

    void recordAll(AuditActivityProvider auditActivityProvider, CourtCaseEntity courtCase);
}
