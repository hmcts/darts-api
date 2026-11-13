package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

public interface CaseLinkImportSummaryService {

    CaseLinkImportResponse getSummary(Long importId);

    CaseLinkImportResponse getSummary(Long importId, PaginationDto<CaseLinkImportFailure> paginationDto);
}
