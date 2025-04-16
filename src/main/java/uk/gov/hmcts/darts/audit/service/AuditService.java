package uk.gov.hmcts.darts.audit.service;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Optional;

@FunctionalInterface
public interface AuditService {
    void recordAudit(AuditActivity activity, UserAccountEntity userAccountEntity, Optional<CourtCaseEntity> courtCase,
                     Optional<String> additionalData);
}