package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportAuditService;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkImportAuditServiceImplTest {

    private static final OffsetDateTime CURRENT_TIME = OffsetDateTime.parse("2026-01-01T12:00:00Z");

    @Mock
    private CaseLinkImportRepository importRepository;
    @Mock
    private CaseLinkImportResultRepository resultRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private EntityManager entityManager;
    private CaseLinkImportAuditService service;
    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();

    @BeforeEach
    void setUp() {
        service = new CaseLinkImportAuditServiceImpl(importRepository, resultRepository, currentTimeHelper, entityManager, configuration);
    }

    @Test
    void failRow_shouldClearRolledBackIdsAndCreationFlags_whenCase2WasCreated() {
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
        verify(entityManager).clear();
    }

    @Test
    void failRow_shouldRetainResolvedCaseIdsAndWarnings_whenNeitherCaseWasCreated() {
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
    void finish_shouldSetStatusAndCompletionTime_whenRowOutcomesAreStored(long failures, CaseLinkImportStatus expected) {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(resultRepository.countByImportIdAndStatusId(1L, CaseLinkResultStatus.FAILED.getId())).thenReturn(failures);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_TIME);
        service.setCompletion(1L);
        assertThat(caseLinkImportEntity.getStatus().getId()).isEqualTo(expected.getId());
        assertThat(caseLinkImportEntity.getProcessingCompletedAt()).isEqualTo(CURRENT_TIME);
    }

    @Test
    void prepare_shouldRecordStartTimeAndRowAuditFields_whenImportIsReadyForProcessing() {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        var row = row();
        UserAccountEntity user = new UserAccountEntity();
        user.setId(123);
        var rows = List.of(row);
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_TIME);
        service.updateCaseLinkImport(1L, rows, user);
        assertThat(caseLinkImportEntity.getStatus().getId()).isEqualTo(CaseLinkImportStatus.PROCESSING.getId());
        assertThat(caseLinkImportEntity.getProcessingStartedAt()).isEqualTo(CURRENT_TIME);
        assertThat(row.getImportId()).isEqualTo(1L);
        assertThat(row.getCreatedById()).isEqualTo(user.getId());
        assertThat(row.getLastModifiedById()).isEqualTo(user.getId());
        var persistenceOrder = inOrder(resultRepository, entityManager);
        persistenceOrder.verify(resultRepository).saveAllAndFlush(rows);
        persistenceOrder.verify(entityManager).clear();
    }

    @ParameterizedTest
    @EnumSource(value = CaseLinkImportStatus.class, names = {"REJECTED", "FAILED"})
    void failImport_shouldRecordStatusReasonAndCompletionTime_whenImportFailsOrIsRejected(CaseLinkImportStatus status) {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_TIME);
        service.failImport(1L, status, "IMPORT_FAILED", "Failure");
        assertThat(caseLinkImportEntity.getStatus().getId()).isEqualTo(status.getId());
        assertThat(caseLinkImportEntity.getErrorCode()).isEqualTo("IMPORT_FAILED");
        assertThat(caseLinkImportEntity.getErrorMessage()).isEqualTo("Failure");
        assertThat(caseLinkImportEntity.getProcessingCompletedAt()).isEqualTo(CURRENT_TIME);
    }

    @DisplayName("""
        updateCaseLinkImport()
        SHOULD retain the import and user audit fields on every row
            AND flush and clear the persistence context after each batch
        WHEN the staged rows span multiple full batches and a partial final batch
        """)
    @ParameterizedTest
    @ValueSource(ints = {2, 1000})
    void updateCaseLinkImport_shouldFlushAndClearEachConfiguredBatch_whenStagedRowsExceedBatchSize(int auditBatchSize) {
        configuration.setAuditBatchSize(auditBatchSize);
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        List<CaseLinkImportResultEntity> stagedRows = IntStream.range(0, auditBatchSize * 2 + 1).mapToObj(index -> row()).toList();
        UserAccountEntity uploadingUser = new UserAccountEntity();
        uploadingUser.setId(123);
        when(importRepository.findById(1L)).thenReturn(Optional.of(caseLinkImportEntity));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_TIME);

        service.updateCaseLinkImport(1L, stagedRows, uploadingUser);

        var persistenceOrder = inOrder(resultRepository, entityManager);
        persistenceOrder.verify(resultRepository).saveAllAndFlush(stagedRows.subList(0, auditBatchSize));
        persistenceOrder.verify(entityManager).clear();
        persistenceOrder.verify(resultRepository).saveAllAndFlush(stagedRows.subList(auditBatchSize, auditBatchSize * 2));
        persistenceOrder.verify(entityManager).clear();
        persistenceOrder.verify(resultRepository).saveAllAndFlush(stagedRows.subList(auditBatchSize * 2, auditBatchSize * 2 + 1));
        persistenceOrder.verify(entityManager).clear();
        persistenceOrder.verifyNoMoreInteractions();
        assertThat(stagedRows).allSatisfy(stagedRow -> {
            assertThat(stagedRow.getImportId()).isEqualTo(1L);
            assertThat(stagedRow.getCreatedById()).isEqualTo(uploadingUser.getId());
            assertThat(stagedRow.getLastModifiedById()).isEqualTo(uploadingUser.getId());
        });
    }

    private CaseLinkImportResultEntity row() {
        CaseLinkImportResultEntity row = new CaseLinkImportResultEntity();
        row.setCase1Id(1);
        row.setCase2Id(2);
        row.setLinkedCaseId(3);
        return row;
    }
}
