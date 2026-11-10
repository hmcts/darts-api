package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailureCode;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportSummaryService;
import uk.gov.hmcts.darts.cases.validator.CaseLinkImportPaginationValidator;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseLinkImportSummaryServiceImpl implements CaseLinkImportSummaryService {

    private final CaseLinkImportRepository caseLinkImportRepository;
    private final CaseLinkImportResultRepository caseLinkImportResultRepository;
    private final CaseLinkImportConfiguration configuration;
    private final CaseLinkImportPaginationValidator paginationValidator;

    @Override
    public CaseLinkImportResponse getSummary(Long importId) {
        return getSummary(importId, new PaginationDto<>(1, null, List.of(), List.of()));
    }

    @Override
    public CaseLinkImportResponse getSummary(Long importId, PaginationDto<CaseLinkImportFailure> paginationDto) {
        PaginationDto<CaseLinkImportFailure> failurePagination = new PaginationDto<>(
            paginationDto.getPageNumber(),
            paginationDto.getPageSize() == null ? configuration.getFailurePageSize() : paginationDto.getPageSize(),
            List.of(), List.of());
        paginationValidator.validate(failurePagination);
        CaseLinkImportEntity caseLinkImportEntity = caseLinkImportRepository.findById(importId)
            .orElseThrow(() -> new DartsApiException(CaseApiError.CASE_LINK_IMPORT_NOT_FOUND));
        CaseLinkImportResponse response = new CaseLinkImportResponse();
        response.setImportId(importId);
        response.setStatus(CaseLinkImportStatus.fromId(caseLinkImportEntity.getStatus().getId()).name());
        response.setTotalRows(Math.toIntExact(caseLinkImportResultRepository.countByImportId(importId)));
        response.setSuccessfulRows(Math.toIntExact(caseLinkImportResultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.SUCCESS.getId())));
        response.setFailedRows(Math.toIntExact(caseLinkImportResultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.FAILED.getId())));
        return failurePagination.toPaginatedList(
            pageable -> pageable.getOffset() >= response.getFailedRows()
                ? new PageImpl<>(List.of(), pageable, response.getFailedRows())
                : caseLinkImportResultRepository.findByImportIdAndStatusId(importId, CaseLinkResultStatus.FAILED.getId(), pageable),
            this::toFailure, List.of("rowNumber"), List.of(Sort.Direction.ASC)
        ).mapToPaginatedListCommon(response, CaseLinkImportResponse::setFailures);
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
