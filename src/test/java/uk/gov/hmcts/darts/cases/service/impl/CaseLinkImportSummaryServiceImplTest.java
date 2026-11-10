package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailureCode;
import uk.gov.hmcts.darts.cases.validator.CaseLinkImportPaginationValidator;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportStatusEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkImportSummaryServiceImplTest {

    @Mock
    private CaseLinkImportRepository importRepository;
    @Mock
    private CaseLinkImportResultRepository resultRepository;
    @Spy
    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
    @Mock
    private CaseLinkImportPaginationValidator paginationValidator;
    @InjectMocks
    private CaseLinkImportSummaryServiceImpl service;

    @DisplayName("""
        getSummary()
        SHOULD derive total, successful and failed row counts from stored results
            AND return the stored import status
            AND include the first page of failed rows with their supplied details, row number, error code and message
            AND return pagination metadata for all failed rows
        WHEN an import contains both successful and failed results
        """)
    @ParameterizedTest
    @EnumSource(CaseLinkImportFailureCode.class)
    void getSummary_shouldReturnStoredOutcomes_whenImportHasMixedResults(CaseLinkImportFailureCode failureCode) {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.COMPLETED_WITH_ERRORS.getId()));
        CaseLinkImportResultEntity row = new CaseLinkImportResultEntity();
        row.setRowNumber(7);
        row.setCase1CaseNumber(" X1 ");
        row.setCase1Courthouse(" york ");
        row.setCase2CaseNumber(" CP1 ");
        row.setErrorCode(failureCode.getValue());
        row.setErrorMessage("Stored row failure message");
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(resultRepository.countByImportId(1L)).thenReturn(3L);
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.SUCCESS.getId())).thenReturn(2L);
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(1L);
        when(resultRepository.findByImportIdAndStatusId(eq(1L), eq(CaseLinkResultStatus.FAILED.getId()), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(row)));
        var summary = service.getSummary(1L);
        assertThat(summary.getImportId()).isEqualTo(1L);
        assertThat(summary.getStatus()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(summary.getTotalRows()).isEqualTo(3);
        assertThat(summary.getSuccessfulRows()).isEqualTo(2);
        assertThat(summary.getFailedRows()).isEqualTo(1);
        assertThat(summary.getCurrentPage()).isEqualTo(1);
        assertThat(summary.getPageSize()).isEqualTo(100);
        assertThat(summary.getTotalItems()).isEqualTo(1);
        assertThat(summary.getTotalPages()).isEqualTo(1);
        assertThat(summary.getFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.getRowNumber()).isEqualTo(7);
            assertThat(failure.getCase1CaseNumber()).isEqualTo(row.getCase1CaseNumber());
            assertThat(failure.getCase1Courthouse()).isEqualTo(row.getCase1Courthouse());
            assertThat(failure.getCase2CaseNumber()).isEqualTo(row.getCase2CaseNumber());
            assertThat(failure.getErrorCode()).isEqualTo(failureCode);
            assertThat(failure.getErrorMessage()).isEqualTo(row.getErrorMessage());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID_REQUEST", "CASE_100", "UNKNOWN_ERROR"})
    void getSummary_shouldReturnGenericFailureAndPreserveMessage_whenStoredErrorCodeIsOutsideRowFailureContract(String storedErrorCode) {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.COMPLETED_WITH_ERRORS.getId()));
        CaseLinkImportResultEntity failedRow = new CaseLinkImportResultEntity();
        failedRow.setErrorCode(storedErrorCode);
        failedRow.setErrorMessage("Original failure details");
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(resultRepository.countByImportId(1L)).thenReturn(1L);
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.SUCCESS.getId())).thenReturn(0L);
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(1L);
        when(resultRepository.findByImportIdAndStatusId(eq(1L), eq(CaseLinkResultStatus.FAILED.getId()), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(failedRow)));

        var summary = service.getSummary(1L);

        assertThat(summary.getFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.getErrorCode()).isEqualTo(CaseLinkImportFailureCode.ROW_PROCESSING_FAILED);
            assertThat(failure.getErrorMessage()).isEqualTo(failedRow.getErrorMessage());
        });
    }

    @Test
    void getSummary_shouldReturnRequestedPageAndWholeImportCounts_whenFailuresSpanManyPages() {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.COMPLETED_WITH_ERRORS.getId()));
        CaseLinkImportResultEntity failedRow = new CaseLinkImportResultEntity();
        failedRow.setRowNumber(1202);
        failedRow.setErrorCode("CASE_1_NOT_FOUND");
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(resultRepository.countByImportId(1L)).thenReturn(1503L);
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.SUCCESS.getId())).thenReturn(3L);
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(1500L);
        when(resultRepository.findByImportIdAndStatusId(eq(1L), eq(CaseLinkResultStatus.FAILED.getId()), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(failedRow), PageRequest.of(12, 100), 1500));
        PaginationDto<CaseLinkImportFailure> paginationDto = new PaginationDto<>(13, 100, List.of(), List.of());

        var summary = service.getSummary(1L, paginationDto);

        assertThat(summary.getTotalRows()).isEqualTo(1503);
        assertThat(summary.getSuccessfulRows()).isEqualTo(3);
        assertThat(summary.getFailedRows()).isEqualTo(1500);
        assertThat(summary.getCurrentPage()).isEqualTo(13);
        assertThat(summary.getPageSize()).isEqualTo(100);
        assertThat(summary.getTotalItems()).isEqualTo(1500);
        assertThat(summary.getTotalPages()).isEqualTo(15);
        assertThat(summary.getFailures()).extracting(CaseLinkImportFailure::getRowNumber).containsExactly(1202);
        ArgumentCaptor<Pageable> pageRequestCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(resultRepository).findByImportIdAndStatusId(eq(1L), eq(CaseLinkResultStatus.FAILED.getId()), pageRequestCaptor.capture());
        assertThat(pageRequestCaptor.getValue().getOffset()).isEqualTo(1200);
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageRequestCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "rowNumber"));
        verify(resultRepository, never()).findByImportIdAndStatusIdOrderByRowNumber(any(), any());
    }

    @Test
    void getSummary_shouldUseConfiguredPageSize_whenPageSizeIsOmitted() {
        configuration.setFailurePageSize(25);
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.COMPLETED.getId()));
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));

        var summary = service.getSummary(1L);

        assertThat(summary.getPageSize()).isEqualTo(25);
        assertThat(summary.getCurrentPage()).isEqualTo(1);
        assertThat(summary.getTotalPages()).isZero();
        assertThat(summary.getTotalItems()).isZero();
        assertThat(summary.getFailures()).isEmpty();
        verify(resultRepository, never()).findByImportIdAndStatusId(any(), any(), any());
    }

    @Test
    void getSummary_shouldRejectRequestWithoutReadingResults_whenImportDoesNotExist() {
        when(importRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(1L))
            .isInstanceOfSatisfying(DartsApiException.class,
                                   exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_LINK_IMPORT_NOT_FOUND));

        verifyNoInteractions(resultRepository);
    }
}
