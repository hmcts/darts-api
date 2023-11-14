package uk.gov.hmcts.darts.audit.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Service
@RequiredArgsConstructor
public class AuditApiImpl {

    private final AuditService auditService;

    public void recordAudit(AuditActivityEnum activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase) {
        auditService.recordAudit(activity, userAccountEntity, courtCase);
    }
}
