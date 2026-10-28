package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
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
class CaseLinkImportSummaryServiceTest {

    @Mock
    private CaseLinkImportRepository importRepository;
    @Mock
    private CaseLinkImportResultRepository resultRepository;
    @InjectMocks
    private CaseLinkImportSummaryService service;

    @Test
    void given_persistedOutcomes_when_regeneratingSummary_then_countRowsAndMapEveryFailureField() {
        var entity = new CaseLinkImportEntity();
        entity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.COMPLETED_WITH_ERRORS.getId()));
        var row = new CaseLinkImportResultEntity();
        row.setRowNumber(7);
        row.setCase1CaseNumber(" X1 ");
        row.setCase1Courthouse(" york ");
        row.setCase2CaseNumber(" CP1 ");
        row.setErrorCode("CASE_1_NOT_FOUND");
        row.setErrorMessage("Case 1 does not exist");
        when(importRepository.findById(1L)).thenReturn(Optional.of(entity));
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
            assertThat(failure.getErrorCode()).isEqualTo(row.getErrorCode());
            assertThat(failure.getErrorMessage()).isEqualTo(row.getErrorMessage());
        });
    }
}
