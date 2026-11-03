package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportAuditService;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CaseLinkImportAuditServiceImpl implements CaseLinkImportAuditService {

    private final CaseLinkImportRepository caseLinkImportRepository;
    private final CaseLinkImportResultRepository caseLinkImportResultRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public CaseLinkImportEntity createCaseLinkImport(String filename, UserAccountEntity user) {
        CaseLinkImportEntity caseLinkImportEntity = new CaseLinkImportEntity();
        caseLinkImportEntity.setOriginalFilename(filename);
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.RECEIVED.getId()));
        caseLinkImportEntity.setCreatedBy(user);
        caseLinkImportEntity.setLastModifiedBy(user);
        return caseLinkImportRepository.saveAndFlush(caseLinkImportEntity);
    }

    @Override
    public void checksum(Long importId, String checksum) {
        caseLinkImportRepository.findById(importId).orElseThrow().setFileChecksum(checksum);
    }

    @Override
    public void updateCaseLinkImport(Long importId, List<CaseLinkImportResultEntity> rows, UserAccountEntity user) {
        CaseLinkImportEntity caseLinkImportEntity = caseLinkImportRepository.findById(importId).orElseThrow();
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(CaseLinkImportStatus.PROCESSING.getId()));
        caseLinkImportEntity.setProcessingStartedAt(currentTimeHelper.currentOffsetDateTime());
        for (CaseLinkImportResultEntity row : rows) {
            row.setImportId(importId);
            row.setCreatedBy(user);
            row.setLastModifiedBy(user);
        }
        caseLinkImportResultRepository.saveAllAndFlush(rows);
    }

    @Override
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
        caseLinkImportResultRepository.saveAndFlush(row);
    }

    @Override
    public void setCompletion(Long importId) {
        CaseLinkImportEntity caseLinkImportEntity = caseLinkImportRepository.findById(importId).orElseThrow();
        CaseLinkImportStatus status = getCompletionStatus(importId);
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(status.getId()));
        caseLinkImportEntity.setProcessingCompletedAt(currentTimeHelper.currentOffsetDateTime());
    }

    private @NonNull CaseLinkImportStatus getCompletionStatus(Long importId) {
        return caseLinkImportResultRepository.countByImportIdAndStatusId(importId, CaseLinkResultStatus.FAILED.getId()) == 0
            ? CaseLinkImportStatus.COMPLETED : CaseLinkImportStatus.COMPLETED_WITH_ERRORS;
    }

    @Override
    public void failImport(Long importId, CaseLinkImportStatus status, String code, String message) {
        CaseLinkImportEntity caseLinkImportEntity = caseLinkImportRepository.findById(importId).orElseThrow();
        caseLinkImportEntity.setStatus(new CaseLinkImportStatusEntity(status.getId()));
        caseLinkImportEntity.setErrorCode(code);
        caseLinkImportEntity.setErrorMessage(message);
        caseLinkImportEntity.setProcessingCompletedAt(currentTimeHelper.currentOffsetDateTime());
    }
}
