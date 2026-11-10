package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailureCode;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportSummaryService;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

@Service
@RequiredArgsConstructor
public class CaseLinkImportSummaryServiceImpl implements CaseLinkImportSummaryService {

    private final CaseLinkImportRepository caseLinkImportRepository;
    private final CaseLinkImportResultRepository caseLinkImportResultRepository;

    @Transactional(readOnly = true)
    @Override
    public CaseLinkImportResponse getSummary(Long importId) {
        CaseLinkImportEntity caseLinkImportEntity = caseLinkImportRepository.findById(importId).orElseThrow();
        CaseLinkImportResponse response = new CaseLinkImportResponse();
        response.setImportId(importId);
        response.setStatus(CaseLinkImportStatus.fromId(caseLinkImportEntity.getStatus().getId()).name());
        response.setTotalRows(Math.toIntExact(caseLinkImportResultRepository.countByImportId(importId)));
        response.setSuccessfulRows(Math.toIntExact(caseLinkImportResultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.SUCCESS.getId())));
        response.setFailedRows(Math.toIntExact(caseLinkImportResultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.FAILED.getId())));
        response.setFailures(caseLinkImportResultRepository.findByImportIdAndStatusIdOrderByRowNumber(importId, CaseLinkResultStatus.FAILED.getId())
                                 .stream().map(this::toFailure).toList());
        return response;
    }

    private CaseLinkImportFailure toFailure(CaseLinkImportResultEntity row) {
        CaseLinkImportFailure failure = new CaseLinkImportFailure();
        failure.setRowNumber(row.getRowNumber());
        failure.setCase1CaseNumber(row.getCase1CaseNumber());
        failure.setCase1Courthouse(row.getCase1Courthouse());
        failure.setCase2CaseNumber(row.getCase2CaseNumber());
        failure.setErrorCode(EnumUtils.getEnum(CaseLinkImportFailureCode.class, row.getErrorCode(), CaseLinkImportFailureCode.ROW_PROCESSING_FAILED));
        failure.setErrorMessage(row.getErrorMessage());
        return failure;
    }
}
