package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.cases.model.GetCasesRequest;

public interface CasesLoggerService {
    void casesRequestedByDarPc(GetCasesRequest getCasesRequest);
}
