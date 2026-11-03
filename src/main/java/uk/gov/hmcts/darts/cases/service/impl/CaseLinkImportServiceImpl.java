package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkCsvService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportAuditService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportSummaryService;
import uk.gov.hmcts.darts.cases.service.CaseLinkRowService;
import uk.gov.hmcts.darts.cases.service.CaseLinkRowValidator;
import uk.gov.hmcts.darts.cases.validator.CaseLinkFileValidator;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseLinkImportServiceImpl implements CaseLinkImportService {

    private final AuthorisationApi authorisationApi;
    private final CaseLinkFileValidator caseLinkFileValidator;
    private final CaseLinkCsvService caseLinkCsvService;
    private final CaseLinkRowValidator caseLinkRowValidator;
    private final CaseLinkRowService caseLinkRowService;
    private final CaseLinkImportAuditService caseLinkImportAuditService;
    private final CaseLinkImportSummaryService caseLinkImportSummaryService;
    private final FileContentChecksum fileContentChecksum;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public CaseLinkImportResponse importCaseLinkingFile(MultipartFile caseLinkingFile) {
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        CaseLinkImportEntity caseLinkImportEntity = caseLinkImportAuditService.createCaseLinkImport(caseLinkingFile.getOriginalFilename(), currentUser);
        Long importId = caseLinkImportEntity.getId();
        List<CaseLinkImportResultEntity> rows = readRows(caseLinkingFile, importId);
        try {
            caseLinkImportAuditService.updateCaseLinkImport(importId, rows, currentUser);
            Set<CaseLinkRowValidator.Mapping> seen = new HashSet<>();
            for (CaseLinkImportResultEntity row : rows) {
                processRow(row, currentUser, seen);
            }
            caseLinkImportAuditService.setCompletion(importId);
        } catch (RuntimeException exception) {
            caseLinkImportAuditService.failImport(importId, CaseLinkImportStatus.FAILED, "IMPORT_FAILED", "The import could not be completed");
            throw exception;
        }
        return caseLinkImportSummaryService.getSummary(importId);
    }

    private List<CaseLinkImportResultEntity> readRows(MultipartFile file, Long importId) {
        try {
            caseLinkFileValidator.validate(file);
            try (var inputStream = file.getInputStream()) {
                caseLinkImportAuditService.checksum(importId, fileContentChecksum.calculate(inputStream));
            }
            return caseLinkCsvService.readCaseLinkCsvFile(file);
        } catch (DartsApiException exception) {
            throw reject(importId, exception);
        } catch (IOException exception) {
            throw reject(importId, new DartsApiException(CaseApiError.FILE_READ_FAILED, exception));
        }
    }

    private DartsApiException reject(Long importId, DartsApiException exception) {
        String errorCode = getErrorCode(exception);
        caseLinkImportAuditService.failImport(importId, CaseLinkImportStatus.REJECTED, errorCode, exception.getMessage());
        exception.getCustomProperties().put("import_id", importId);
        exception.getCustomProperties().put("error_code", errorCode);
        exception.getCustomProperties().put("error_message", exception.getMessage());
        return exception;
    }

    private void processRow(CaseLinkImportResultEntity row, UserAccountEntity user, Set<CaseLinkRowValidator.Mapping> seen) {
        try {
            caseLinkRowValidator.validate(row, seen);
            caseLinkRowService.linkCaseToCase(row, user);
        } catch (DartsApiException exception) {
            caseLinkImportAuditService.failRow(row, getErrorCode(exception), exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Case link import {} row {} failed with {}", row.getImportId(), row.getRowNumber(), exception.getClass().getSimpleName());
            caseLinkImportAuditService.failRow(row, "ROW_PROCESSING_FAILED", "The row could not be processed; its case and link changes were rolled back");
        }
    }

    private String getErrorCode(DartsApiException exception) {
        return exception.getError() instanceof CaseApiError error ? error.name() : exception.getError().getType();
    }
}
