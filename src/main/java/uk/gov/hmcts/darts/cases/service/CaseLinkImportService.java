package uk.gov.hmcts.darts.cases.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseLinkImportService {

    private final UserIdentity userIdentity;
    private final CaseLinkCsvService csvService;
    private final CaseLinkRowValidator validator;
    private final CaseLinkRowService rowService;
    private final CaseLinkImportAuditService auditService;
    private final CaseLinkImportSummaryService summaryService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CaseLinkImportResponse importFile(MultipartFile file, int fileCount) {
        var user = userIdentity.getUserAccount();
        var entity = auditService.create(file.getOriginalFilename(), user);
        Long importId = entity.getId();
        List<CaseLinkImportResultEntity> rows = readRows(file, fileCount, importId);
        try {
            auditService.prepare(importId, rows, user);
            Set<CaseLinkRowValidator.Mapping> seen = new HashSet<>();
            for (var row : rows) {
                processRow(row, user, seen);
            }
            auditService.finish(importId);
        } catch (RuntimeException exception) {
            auditService.failImport(importId, CaseLinkImportStatus.FAILED, "IMPORT_FAILED", "The import could not be completed");
            throw exception;
        }
        return summaryService.getSummary(importId);
    }

    private List<CaseLinkImportResultEntity> readRows(MultipartFile file, int fileCount, Long importId) {
        try {
            try (var stream = file.getInputStream()) {
                auditService.checksum(importId, DigestUtils.sha256Hex(stream));
            }
            validateFileCount(fileCount);
            return csvService.read(file);
        } catch (CaseLinkRowException exception) {
            throw reject(importId, exception);
        } catch (IOException exception) {
            throw reject(importId, new CaseLinkRowException("FILE_READ_FAILED", "The uploaded file could not be read", exception));
        }
    }

    private void validateFileCount(int fileCount) {
        if (fileCount != 1) {
            throw new CaseLinkRowException("INVALID_FILE_COUNT", "Exactly one CSV file is required");
        }
    }

    private DartsApiException reject(Long importId, CaseLinkRowException cause) {
        auditService.failImport(importId, CaseLinkImportStatus.REJECTED, cause.getErrorCode(), cause.getMessage());
        var exception = new DartsApiException(CaseApiError.INVALID_REQUEST, cause);
        exception.getCustomProperties().put("import_id", importId);
        exception.getCustomProperties().put("error_code", cause.getErrorCode());
        exception.getCustomProperties().put("error_message", cause.getMessage());
        return exception;
    }

    private void processRow(CaseLinkImportResultEntity row, UserAccountEntity user, Set<CaseLinkRowValidator.Mapping> seen) {
        try {
            validator.validate(row, seen);
            rowService.process(row, user);
        } catch (CaseLinkRowException exception) {
            auditService.failRow(row, exception.getErrorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Case link import {} row {} failed with {}", row.getImportId(), row.getRowNumber(), exception.getClass().getSimpleName());
            auditService.failRow(row, "ROW_PROCESSING_FAILED", "The row could not be processed; its case and link changes were rolled back");
        }
    }
}
