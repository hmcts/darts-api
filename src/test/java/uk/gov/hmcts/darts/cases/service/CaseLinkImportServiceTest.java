package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkImportServiceTest {

    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CaseLinkCsvService csvService;
    @Mock
    private CaseLinkRowValidator validator;
    @Mock
    private CaseLinkRowService rowService;
    @Mock
    private CaseLinkImportAuditService auditService;
    @Mock
    private CaseLinkImportSummaryService summaryService;
    @InjectMocks
    private CaseLinkImportService service;

    private final UserAccountEntity user = new UserAccountEntity();
    private final MockMultipartFile file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[]{1});

    @BeforeEach
    void setUp() {
        when(userIdentity.getUserAccount()).thenReturn(user);
        var entity = new CaseLinkImportEntity();
        entity.setId(1L);
        when(auditService.create("cases.csv", user)).thenReturn(entity);
    }

    @Test
    void given_rowValidationAndProcessingErrors_when_importing_then_auditFailuresAndContinue() {
        var invalid = new CaseLinkImportResultEntity();
        var unexpected = new CaseLinkImportResultEntity();
        var valid = new CaseLinkImportResultEntity();
        when(csvService.read(file)).thenReturn(List.of(invalid, unexpected, valid));
        doThrow(new CaseLinkRowException("DUPLICATE_MAPPING", "Duplicate")).when(validator).validate(eq(invalid), any());
        doThrow(new IllegalStateException("internal error")).when(rowService).process(unexpected, user);
        var summary = new CaseLinkImportResponse();
        when(summaryService.getSummary(1L)).thenReturn(summary);
        assertThat(service.importFile(file, 1)).isSameAs(summary);
        verify(auditService).failRow(invalid, "DUPLICATE_MAPPING", "Duplicate");
        verify(auditService).failRow(eq(unexpected), eq("ROW_PROCESSING_FAILED"), any());
        verify(rowService, never()).process(invalid, user);
        verify(rowService).process(valid, user);
        verify(auditService).finish(1L);
    }

    @Test
    void given_rejectedFile_when_importing_then_auditRejectionAndNeverProcessRows() {
        when(csvService.read(file)).thenThrow(new CaseLinkRowException("MALFORMED_CSV", "Malformed"));
        assertThatThrownBy(() -> service.importFile(file, 1)).isInstanceOf(DartsApiException.class);
        verify(auditService).failImport(1L, CaseLinkImportStatus.REJECTED, "MALFORMED_CSV", "Malformed");
        verify(rowService, never()).process(any(), any());
    }

    @Test
    void given_multipleFiles_when_importing_then_auditRejection() {
        assertThatThrownBy(() -> service.importFile(file, 2)).isInstanceOf(DartsApiException.class);
        verify(auditService).failImport(eq(1L), eq(CaseLinkImportStatus.REJECTED), eq("INVALID_FILE_COUNT"), any());
        verify(csvService, never()).read(any());
    }

    @Test
    void given_failureToPersistAudit_when_importing_then_markImportFailed() {
        when(csvService.read(file)).thenReturn(List.of());
        doThrow(new IllegalStateException("storage unavailable")).when(auditService).prepare(eq(1L), any(), eq(user));
        assertThatThrownBy(() -> service.importFile(file, 1)).isInstanceOf(IllegalStateException.class);
        verify(auditService).failImport(eq(1L), eq(CaseLinkImportStatus.FAILED), eq("IMPORT_FAILED"), any());
        verify(auditService, never()).finish(any());
    }
}
