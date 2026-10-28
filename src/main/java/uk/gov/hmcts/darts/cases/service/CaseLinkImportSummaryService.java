package uk.gov.hmcts.darts.cases.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

@Service
@RequiredArgsConstructor
public class CaseLinkImportSummaryService {

    private final CaseLinkImportRepository importRepository;
    private final CaseLinkImportResultRepository resultRepository;

    @Transactional(readOnly = true)
    public CaseLinkImportResponse getSummary(Long importId) {
        var entity = importRepository.findById(importId).orElseThrow();
        var response = new CaseLinkImportResponse();
        response.setImportId(importId);
        response.setStatus(CaseLinkImportStatus.fromId(entity.getStatus().getId()).name());
        response.setTotalRows(Math.toIntExact(resultRepository.countByImportId(importId)));
        response.setSuccessfulRows(Math.toIntExact(resultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.SUCCESS.getId())));
        response.setFailedRows(Math.toIntExact(resultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.FAILED.getId())));
        response.setFailures(resultRepository.findByImportIdAndStatusIdOrderByRowNumber(importId, CaseLinkResultStatus.FAILED.getId())
                                 .stream().map(this::toFailure).toList());
        return response;
    }

    private CaseLinkImportFailure toFailure(CaseLinkImportResultEntity row) {
        var failure = new CaseLinkImportFailure();
        failure.setRowNumber(row.getRowNumber());
        failure.setCase1CaseNumber(row.getCase1CaseNumber());
        failure.setCase1Courthouse(row.getCase1Courthouse());
        failure.setCase2CaseNumber(row.getCase2CaseNumber());
        failure.setErrorCode(row.getErrorCode());
        failure.setErrorMessage(row.getErrorMessage());
        return failure;
    }
}
