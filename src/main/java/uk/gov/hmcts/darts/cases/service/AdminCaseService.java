package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

public interface AdminCaseService {

    PaginatedList<AdminCaseAudioResponseItem> getAudiosByCaseId(Integer caseId,
                                                                PaginationDto<AdminCaseAudioResponseItem> paginationDto);
}
