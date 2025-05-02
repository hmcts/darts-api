package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.cases.model.AdminSingleCaseResponseItem;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;

public interface CaseService {

    List<ScheduledCase> getHearings(GetCasesRequest request);

    PostCaseResponse addCaseOrUpdate(AddCaseRequest addCaseRequest);

    List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request);

    List<Hearing> getCaseHearings(Integer caseId);

    SingleCase getCasesById(Integer caseId);

    CourtCaseEntity getCourtCaseById(Integer caseId);

    PaginatedList<Event> getEventsByCaseId(Integer caseId, PaginationDto<Event> paginationDto);

    List<Transcript> getTranscriptsByCaseId(Integer caseId);

    List<Annotation> getAnnotations(Integer caseId);

    List<AdminCasesSearchResponseItem> adminCaseSearch(AdminCasesSearchRequest request);

    CourtCaseEntity saveCase(CourtCaseEntity courtCase);

    AdminSingleCaseResponseItem adminGetCaseById(Integer caseId);
}