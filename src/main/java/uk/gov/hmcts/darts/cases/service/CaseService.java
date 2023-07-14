package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;

import java.util.List;

public interface CaseService {

    List<ScheduledCase> getCases(GetCasesRequest request);

    ScheduledCase addCaseOrUpdate(AddCaseRequest addCaseRequest);

    List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request);

    List<Hearing> getCaseHearings(Integer caseId);

}
