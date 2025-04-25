package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

// TODO this suppression can be removed when the CaseService interface is refactored so admin case processing is in here
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AdminCaseService {

    PaginatedList<AdminCaseAudioResponseItem> getAudiosByCaseId(Integer caseId,
                                                                PaginationDto<AdminCaseAudioResponseItem> paginationDto);
}
