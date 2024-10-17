package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface DataAnonymisationService {

    void anonymizeCourtCaseById(UserAccountEntity userAccount, Integer courtCaseId);
}
