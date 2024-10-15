package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface DataAnonymisationService {

    void anonymizeCourtCaseById(Integer courtCaseId);

    UserAccountEntity getUserAccount();
}
