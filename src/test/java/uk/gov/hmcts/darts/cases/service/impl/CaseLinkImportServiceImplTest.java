package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailureCode;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkCsvService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportAuditService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportSummaryService;
import uk.gov.hmcts.darts.cases.service.CaseLinkRowService;
import uk.gov.hmcts.darts.cases.service.CaseLinkRowValidator;
import uk.gov.hmcts.darts.cases.validator.CaseLinkFileValidator;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkImportServiceImplTest {

    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private CaseLinkFileValidator fileValidator;
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
    @Mock
    private FileContentChecksum fileContentChecksum;
    @Mock
    private InputStream checksumInputStream;
    @InjectMocks
    private CaseLinkImportServiceImpl service;

    private final UserAccountEntity user = new UserAccountEntity();
    private final MockMultipartFile file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[]{1});

    @DisplayName("""
        importCaseLinkingFile()
        SHOULD audit row validation failures with their API error
            AND audit unexpected processing failures with a generic error
            AND process subsequent valid rows
            AND finish the import and return its summary
        WHEN different rows fail validation and processing
        """)
    @Test
    void importCaseLinkingFile_shouldContinueProcessing_whenIndividualRowsFail() {
        givenImportAudit();
        when(fileContentChecksum.calculate(any(InputStream.class))).thenReturn("file-checksum");
        CaseLinkImportResultEntity invalid = new CaseLinkImportResultEntity();
        CaseLinkImportResultEntity unexpected = new CaseLinkImportResultEntity();
        CaseLinkImportResultEntity valid = new CaseLinkImportResultEntity();
        when(csvService.readCaseLinkCsvFile(file)).thenReturn(List.of(invalid, unexpected, valid));
        doThrow(new DartsApiException(CaseApiError.DUPLICATE_MAPPING)).when(validator).validate(eq(invalid), any());
        doThrow(new IllegalStateException("internal error")).when(rowService).linkCaseToCase(unexpected, user);
        var summary = new CaseLinkImportResponse();
        when(summaryService.getSummary(1L)).thenReturn(summary);
        assertThat(service.importCaseLinkingFile(file)).isSameAs(summary);
        var ordered = inOrder(fileValidator, authorisationApi, auditService, fileContentChecksum, csvService);
        ordered.verify(authorisationApi).getCurrentUser();
        ordered.verify(auditService).createCaseLinkImport("cases.csv", user);
        ordered.verify(fileValidator).validate(file);
        ordered.verify(fileContentChecksum).calculate(any(InputStream.class));
        ordered.verify(auditService).checksum(1L, "file-checksum");
        ordered.verify(csvService).readCaseLinkCsvFile(file);
        verify(auditService).failRow(invalid, "DUPLICATE_MAPPING", CaseApiError.DUPLICATE_MAPPING.getTitle());
        verify(auditService).failRow(eq(unexpected), eq("ROW_PROCESSING_FAILED"), any());
        verify(rowService, never()).linkCaseToCase(invalid, user);
        verify(rowService).linkCaseToCase(valid, user);
        verify(auditService).setCompletion(1L);
    }

    @ParameterizedTest
    @EnumSource(value = CaseApiError.class, names = {"MALFORMED_CSV", "FILE_READ_FAILED", "TOO_MANY_ROWS"})
    void importCaseLinkingFile_shouldAuditRejectionWithoutProcessingRows_whenCsvCannotBeParsed(CaseApiError error) {
        givenImportAudit();
        var failure = new DartsApiException(error);
        when(csvService.readCaseLinkCsvFile(file)).thenThrow(failure);
        assertThatThrownBy(() -> service.importCaseLinkingFile(file)).isSameAs(failure);
        assertThat(failure.getCustomProperties()).containsEntry("import_id", 1L).containsEntry("error_code", error.name());
        verify(auditService).failImport(1L, CaseLinkImportStatus.REJECTED, error.name(), error.getTitle());
        verify(rowService, never()).linkCaseToCase(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = CaseApiError.class, names = {"INVALID_FILE_TYPE", "INVALID_CONTENT_TYPE", "EMPTY_FILE", "FILE_TOO_LARGE"})
    void importCaseLinkingFile_shouldAuditRejectionBeforeReadingContents_whenBasicFileValidationFails(CaseApiError error) {
        givenImportAudit();
        var failure = new DartsApiException(error);
        doThrow(failure).when(fileValidator).validate(file);
        assertThatThrownBy(() -> service.importCaseLinkingFile(file)).isSameAs(failure);
        assertThat(failure.getCustomProperties()).containsEntry("import_id", 1L)
            .containsEntry("error_code", error.name()).containsEntry("error_message", error.getTitle());
        var ordered = inOrder(authorisationApi, auditService, fileValidator);
        ordered.verify(authorisationApi).getCurrentUser();
        ordered.verify(auditService).createCaseLinkImport("cases.csv", user);
        ordered.verify(fileValidator).validate(file);
        ordered.verify(auditService).failImport(1L, CaseLinkImportStatus.REJECTED, error.name(), error.getTitle());
        verify(auditService, never()).checksum(any(), any());
        verify(auditService, never()).updateCaseLinkImport(any(), any(), any());
        verifyNoInteractions(fileContentChecksum, csvService, validator, rowService, summaryService);
    }

    @Test
    void importCaseLinkingFile_shouldAuditRejectionAndPreserveCause_whenFileCannotBeRead() throws IOException {
        givenImportAudit();
        var unreadable = spy(file);
        var failure = new IOException("Unreadable upload");
        doThrow(failure).when(unreadable).getInputStream();
        assertThatThrownBy(() -> service.importCaseLinkingFile(unreadable))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.FILE_READ_FAILED);
                assertThat(exception.getCause()).isSameAs(failure);
                assertThat(exception.getCustomProperties()).containsEntry("import_id", 1L);
            });
        verify(auditService).failImport(1L, CaseLinkImportStatus.REJECTED, "FILE_READ_FAILED", CaseApiError.FILE_READ_FAILED.getTitle());
        verifyNoInteractions(csvService, validator, rowService);
    }

    @ParameterizedTest
    @EnumSource(value = CaseApiError.class, names = {"INVALID_REQUEST", "MALFORMED_CSV"})
    void importCaseLinkingFile_shouldAuditGenericRowFailureAndContinue_whenCaseCreationThrowsUnrelatedCaseApiError(CaseApiError error) {
        assertUnexpectedApiErrorIsAudited(new DartsApiException(error));
    }

    @Test
    void importCaseLinkingFile_shouldAuditGenericRowFailureAndContinue_whenCaseCreationThrowsCommonApiError() {
        assertUnexpectedApiErrorIsAudited(new DartsApiException(CommonApiError.INVALID_REQUEST));
    }

    @ParameterizedTest
    @EnumSource(value = CaseLinkImportFailureCode.class, names = "ROW_PROCESSING_FAILED", mode = EnumSource.Mode.EXCLUDE)
    void importCaseLinkingFile_shouldPreserveRowFailureCode_whenRowThrowsDocumentedApiError(CaseLinkImportFailureCode failureCode) {
        givenImportAudit();
        CaseLinkImportResultEntity failedRow = new CaseLinkImportResultEntity();
        when(csvService.readCaseLinkCsvFile(file)).thenReturn(List.of(failedRow));
        CaseApiError apiError = CaseApiError.valueOf(failureCode.name());
        doThrow(new DartsApiException(apiError)).when(rowService).linkCaseToCase(failedRow, user);

        service.importCaseLinkingFile(file);

        verify(auditService).failRow(failedRow, failureCode.getValue(), apiError.getTitle());
        verify(auditService).setCompletion(1L);
    }

    private void assertUnexpectedApiErrorIsAudited(DartsApiException exception) {
        givenImportAudit();
        CaseLinkImportResultEntity failed = new CaseLinkImportResultEntity();
        CaseLinkImportResultEntity valid = new CaseLinkImportResultEntity();
        when(csvService.readCaseLinkCsvFile(file)).thenReturn(List.of(failed, valid));
        doThrow(exception).when(rowService).linkCaseToCase(failed, user);
        service.importCaseLinkingFile(file);
        verify(auditService).failRow(failed, "ROW_PROCESSING_FAILED", exception.getMessage());
        verify(rowService).linkCaseToCase(valid, user);
        verify(auditService).setCompletion(1L);
    }

    @Test
    void importCaseLinkingFile_shouldAuditRejectionAndCloseStream_whenChecksumCannotBeCalculated() throws IOException {
        givenImportAudit();
        var unreadableFile = spy(file);
        IOException checksumFailure = new IOException("Cannot read checksum contents");
        doReturn(checksumInputStream).when(unreadableFile).getInputStream();
        doAnswer(invocation -> {
            throw checksumFailure;
        }).when(fileContentChecksum).calculate(checksumInputStream);

        assertThatThrownBy(() -> service.importCaseLinkingFile(unreadableFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.FILE_READ_FAILED);
                assertThat(exception.getCause()).isSameAs(checksumFailure);
                assertThat(exception.getCustomProperties()).containsEntry("import_id", 1L);
            });

        verify(checksumInputStream).close();
        verify(auditService).failImport(1L, CaseLinkImportStatus.REJECTED, "FILE_READ_FAILED", CaseApiError.FILE_READ_FAILED.getTitle());
        verify(auditService, never()).checksum(any(), any());
        verifyNoInteractions(csvService, validator, rowService, summaryService);
    }

    @DisplayName("""
        importCaseLinkingFile()
        SHOULD close the upload stream
            AND mark the import FAILED with IMPORT_FAILED
            AND rethrow the original database exception
            AND stop before parsing or processing rows
        WHEN persisting the calculated checksum fails
        """)
    @Test
    void importCaseLinkingFile_shouldMarkImportFailedAndCloseStream_whenChecksumCannotBePersisted() throws IOException {
        givenImportAudit();
        var uploadedFile = spy(file);
        var checksumPersistenceFailure = new TransientDataAccessResourceException("Checksum storage unavailable");
        doReturn(checksumInputStream).when(uploadedFile).getInputStream();
        when(fileContentChecksum.calculate(checksumInputStream)).thenReturn("file-checksum");
        doThrow(checksumPersistenceFailure).when(auditService).checksum(1L, "file-checksum");

        assertThatThrownBy(() -> service.importCaseLinkingFile(uploadedFile)).isSameAs(checksumPersistenceFailure);

        var processingOrder = inOrder(fileValidator, fileContentChecksum, auditService, checksumInputStream);
        processingOrder.verify(auditService).createCaseLinkImport("cases.csv", user);
        processingOrder.verify(fileValidator).validate(uploadedFile);
        processingOrder.verify(fileContentChecksum).calculate(checksumInputStream);
        processingOrder.verify(auditService).checksum(1L, "file-checksum");
        processingOrder.verify(checksumInputStream).close();
        processingOrder.verify(auditService).failImport(1L, CaseLinkImportStatus.FAILED, "IMPORT_FAILED", "The import could not be completed");
        verifyNoMoreInteractions(auditService);
        verifyNoInteractions(csvService, validator, rowService, summaryService);
    }

    @DisplayName("""
        importCaseLinkingFile()
        SHOULD mark the import FAILED with IMPORT_FAILED
            AND rethrow the original exception
            AND stop before preparing or processing row audits
        WHEN CSV preprocessing throws an unexpected runtime exception
        """)
    @Test
    void importCaseLinkingFile_shouldMarkImportFailed_whenCsvPreprocessingFailsUnexpectedly() {
        givenImportAudit();
        var csvPreprocessingFailure = new IllegalStateException("Unexpected CSV preprocessing failure");
        when(fileContentChecksum.calculate(any(InputStream.class))).thenReturn("file-checksum");
        when(csvService.readCaseLinkCsvFile(file)).thenThrow(csvPreprocessingFailure);

        assertThatThrownBy(() -> service.importCaseLinkingFile(file)).isSameAs(csvPreprocessingFailure);

        var processingOrder = inOrder(auditService, csvService);
        processingOrder.verify(auditService).createCaseLinkImport("cases.csv", user);
        processingOrder.verify(auditService).checksum(1L, "file-checksum");
        processingOrder.verify(csvService).readCaseLinkCsvFile(file);
        processingOrder.verify(auditService).failImport(1L, CaseLinkImportStatus.FAILED, "IMPORT_FAILED", "The import could not be completed");
        verifyNoMoreInteractions(auditService);
        verifyNoInteractions(validator, rowService, summaryService);
    }

    @Test
    void importCaseLinkingFile_shouldMarkImportFailed_whenPreparingAuditRowsFails() {
        givenImportAudit();
        when(csvService.readCaseLinkCsvFile(file)).thenReturn(List.of());
        doThrow(new IllegalStateException("storage unavailable")).when(auditService).updateCaseLinkImport(eq(1L), any(), eq(user));
        assertThatThrownBy(() -> service.importCaseLinkingFile(file)).isInstanceOf(IllegalStateException.class);
        verify(auditService).failImport(eq(1L), eq(CaseLinkImportStatus.FAILED), eq("IMPORT_FAILED"), any());
        verify(auditService, never()).setCompletion(any());
    }

    private void givenImportAudit() {
        when(authorisationApi.getCurrentUser()).thenReturn(user);
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        caseLinkImportEntity.setId(1L);
        when(auditService.createCaseLinkImport("cases.csv", user)).thenReturn(caseLinkImportEntity);
    }
}
