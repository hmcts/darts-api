package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface DataAnonymisationService {

    void anonymizeCourtCaseById(UserAccountEntity userAccount, Integer courtCaseId);

    void anonymizeCourtCaseEntity(UserAccountEntity userAccount, CourtCaseEntity courtCase);
  
    void obfuscateEventByIds(List<Integer> eveIds);
  
    void anonymizeEvent(EventEntity eventEntity);

    UserAccountEntity getUserAccount();
}
