package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;

public interface CasesLoggerService {
    void casesRequestedByDarPc(GetCasesRequest getCasesRequest);

    void defendantNameOverflow(AddCaseRequest addCaseRequest);

    void defendantNotAdded(String defendant, String caseNumber);

    void caseDeletedDueToExpiry(Integer caseId, String caseNumber);
}
