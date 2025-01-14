package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;

import java.util.List;

public interface RetentionService {

    List<GetCaseRetentionsResponse> getCaseRetentions(Integer caseId);

    CourtCaseEntity updateCourtCaseConfidenceAttributesForRetention(CourtCaseEntity courtCase, RetentionConfidenceCategoryEnum confidenceCategory);

}
