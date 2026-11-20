package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface CaseLinkImportAuditService {

    CaseLinkImportEntity createCaseLinkImport(String filename, UserAccountEntity user);

    void checksum(Long importId, String checksum);

    void updateCaseLinkImport(Long importId, List<CaseLinkImportResultEntity> rows, UserAccountEntity user);

    void failRow(CaseLinkImportResultEntity row, String code, String message);

    void setCompletion(Long importId);

    void failImport(Long importId, CaseLinkImportStatus status, String code, String message);
}
