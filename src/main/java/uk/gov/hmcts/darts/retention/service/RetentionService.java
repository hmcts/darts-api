package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.retentions.model.CaseRetention;

import java.util.List;

public interface RetentionService {

    List<CaseRetention> getCaseRetentions(Integer caseId);
}
