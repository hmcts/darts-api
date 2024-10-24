package uk.gov.hmcts.darts.audit.service;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface AuditService {

    void recordAudit(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase);

    void recordAudit(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase, String additionalData);
}
