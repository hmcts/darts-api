package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkImportAuditServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CaseLinkImportRepository importRepository;
    @Mock
    private CaseLinkImportResultRepository resultRepository;
    private CaseLinkImportAuditService service;

    @BeforeEach
    void setUp() {
        service = new CaseLinkImportAuditService(importRepository, resultRepository, CLOCK);
    }

    @Test
    void given_rolledBackCreation_when_recordingFailure_then_clearUncommittedIdsAndCreationFlags() {
        var row = row();
        row.setCase2Created(true);
        service.failRow(row, "ROW_PROCESSING_FAILED", "Failure");
        assertThat(row.getCase1Id()).isEqualTo(1);
        assertThat(row.getCase2Id()).isNull();
        assertThat(row.getLinkedCaseId()).isNull();
        assertThat(row.isCase1Created()).isFalse();
        assertThat(row.isCase2Created()).isFalse();
        assertThat(row.getStatus().getId()).isEqualTo(CaseLinkResultStatus.FAILED.getId());
        assertThat(row.getErrorCode()).isEqualTo("ROW_PROCESSING_FAILED");
        assertThat(row.getErrorMessage()).isEqualTo("Failure");
        verify(resultRepository).saveAndFlush(row);
    }

    @Test
    void given_existingCases_when_recordingFailure_then_retainResolvedIdsAndWarnings() {
        var row = row();
        row.setWarningCodes("CASE_1_DELETED");
        service.failRow(row, "SELF_LINK", "Cannot link to itself");
        assertThat(row.getCase1Id()).isEqualTo(1);
        assertThat(row.getCase2Id()).isEqualTo(2);
        assertThat(row.getWarningCodes()).isEqualTo("CASE_1_DELETED");
        assertThat(row.getLinkedCaseId()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"0,COMPLETED", "1,COMPLETED_WITH_ERRORS"})
    void given_storedRowOutcomes_when_finishing_then_deriveWorkflowStatus(long failures, CaseLinkImportStatus expected) {
        var entity = new CaseLinkImportEntity();
        when(importRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(failures);
        service.finish(1L);
        assertThat(entity.getStatus().getId()).isEqualTo(expected.getId());
        assertThat(entity.getProcessingCompletedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
    }

    private CaseLinkImportResultEntity row() {
        var row = new CaseLinkImportResultEntity();
        row.setCase1Id(1);
        row.setCase2Id(2);
        row.setLinkedCaseId(3);
        return row;
    }
}
