package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailureCode;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportStatusEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkImportSummaryServiceImplTest {

    @Mock
    private CaseLinkImportRepository importRepository;
    @Mock
    private CaseLinkImportResultRepository resultRepository;
    @InjectMocks
    private CaseLinkImportSummaryServiceImpl service;

    @DisplayName("""
        getSummary()
        SHOULD derive total, successful and failed row counts from stored results
            AND return the stored import status
            AND include every failed row with its supplied case details, row number, error code and message
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
        when(resultRepository.findByImportIdAndStatusIdOrderByRowNumber(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(List.of(row));
        var summary = service.getSummary(1L);
        assertThat(summary.getImportId()).isEqualTo(1L);
        assertThat(summary.getStatus()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(summary.getTotalRows()).isEqualTo(3);
        assertThat(summary.getSuccessfulRows()).isEqualTo(2);
        assertThat(summary.getFailedRows()).isEqualTo(1);
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
        when(resultRepository.findByImportIdAndStatusIdOrderByRowNumber(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(List.of(failedRow));

        var summary = service.getSummary(1L);

        assertThat(summary.getFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.getErrorCode()).isEqualTo(CaseLinkImportFailureCode.ROW_PROCESSING_FAILED);
            assertThat(failure.getErrorMessage()).isEqualTo(failedRow.getErrorMessage());
        });
    }
}
