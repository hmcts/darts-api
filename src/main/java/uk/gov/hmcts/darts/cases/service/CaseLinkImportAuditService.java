package uk.gov.hmcts.darts.cases.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CaseLinkImportAuditService {

    private final CaseLinkImportRepository importRepository;
    private final CaseLinkImportResultRepository resultRepository;
    private final Clock clock;

    public CaseLinkImportEntity create(String filename, UserAccountEntity user) {
        var entity = new CaseLinkImportEntity();
        entity.setOriginalFilename(filename);
        entity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.RECEIVED.getId()));
        entity.setCreatedBy(user);
        entity.setLastModifiedBy(user);
        return importRepository.saveAndFlush(entity);
    }

    public void checksum(Long importId, String checksum) {
        importRepository.findById(importId).orElseThrow().setFileChecksum(checksum);
    }

    public void prepare(Long importId, List<CaseLinkImportResultEntity> rows, UserAccountEntity user) {
        var entity = importRepository.findById(importId).orElseThrow();
        entity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.PROCESSING.getId()));
        entity.setProcessingStartedAt(OffsetDateTime.now(clock));
        for (var row : rows) {
            row.setImportId(importId);
            row.setCreatedBy(user);
            row.setLastModifiedBy(user);
        }
        resultRepository.saveAllAndFlush(rows);
    }

    public void failRow(CaseLinkImportResultEntity row, String code, String message) {
        // Any IDs allocated for a rolled-back case or link must not escape into the audit.
        if (row.isCase2Created()) {
            row.setCase2Id(null);
        }
        row.setCase1Created(false);
        row.setCase2Created(false);
        row.setLinkedCaseId(null);
        row.setStatus(new CaseLinkImportResultStatusEntity(CaseLinkResultStatus.FAILED.getId()));
        row.setErrorCode(code);
        row.setErrorMessage(message);
        resultRepository.saveAndFlush(row);
    }

    public void finish(Long importId) {
        var entity = importRepository.findById(importId).orElseThrow();
        var status = resultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.FAILED.getId()) == 0
            ? CaseLinkImportStatus.COMPLETED : CaseLinkImportStatus.COMPLETED_WITH_ERRORS;
        entity.setStatus(new CaseLinkImportStatusEntity(status.getId()));
        entity.setProcessingCompletedAt(OffsetDateTime.now(clock));
    }

    public void failImport(Long importId, CaseLinkImportStatus status, String code, String message) {
        var entity = importRepository.findById(importId).orElseThrow();
        entity.setStatus(new CaseLinkImportStatusEntity(status.getId()));
        entity.setErrorCode(code);
        entity.setErrorMessage(message);
        entity.setProcessingCompletedAt(OffsetDateTime.now(clock));
    }
}
