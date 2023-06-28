package uk.gov.hmcts.darts.audit.service;

import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.common.entity.AuditEntity;

import java.util.List;

public interface AuditService {
    List<AuditEntity> search(AuditSearchQuery auditSearchQuery);
}
