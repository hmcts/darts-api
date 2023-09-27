package uk.gov.hmcts.darts.authorisation.component;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface UserIdentity {

    String getEmailAddress();

    UserAccountEntity getUserAccount();

}
