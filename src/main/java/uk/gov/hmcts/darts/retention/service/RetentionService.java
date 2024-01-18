package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;

import java.util.List;

public interface RetentionService {

    List<GetCaseRetentionsResponse> getCaseRetentions(Integer caseId);
}
