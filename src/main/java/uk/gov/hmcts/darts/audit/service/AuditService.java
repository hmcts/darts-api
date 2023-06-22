package uk.gov.hmcts.darts.audit.service;

import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.common.entity.Audit;

import java.util.List;

public interface AuditService {
    List<Audit> search(AuditSearchQuery auditSearchQuery);
}
