package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;

@FunctionalInterface
public interface CaseLinkImportSummaryService {

    CaseLinkImportResponse getSummary(Long importId);
}
