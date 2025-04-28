package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface DataAnonymisationService {

    void anonymiseCourtCaseById(UserAccountEntity userAccount, Integer courtCaseId, boolean isManuallyRequested);

    void anonymiseEventByIds(UserAccountEntity userAccount, List<Long> eveIds, boolean isManuallyRequested);

    void anonymiseEvent(UserAccountEntity userAccount, EventEntity eventEntity, boolean isManuallyRequested);
}
