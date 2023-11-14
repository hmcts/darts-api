package uk.gov.hmcts.darts.audit.api;

import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface AuditApi {

    public void recordAudit(AuditActivityEnum activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase);
}
