package uk.gov.hmcts.darts.audit.service;

import jakarta.transaction.Transactional;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface AuditService {

    void recordAuditRequestAudio(AuditActivityEnum activity, Integer userAccountEntity, Integer courtCase);

    @Transactional
    void recordAudit(AuditActivityEnum activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase);

    void recordAuditDownload(AuditActivityEnum activity, Integer userId, Integer hearingId);

    List<AuditEntity> search(AuditSearchQuery auditSearchQuery);
}
