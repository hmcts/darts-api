package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@FunctionalInterface
public interface CaseLinkRowService {

    void linkCaseToCase(CaseLinkImportResultEntity row, UserAccountEntity user);
}
