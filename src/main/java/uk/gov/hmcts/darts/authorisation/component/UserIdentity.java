package uk.gov.hmcts.darts.authorisation.component;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

public interface UserIdentity {

    String getEmailAddress();

    String getGuidFromToken();

    UserAccountEntity getUserAccount();

    boolean userHasGlobalAccess(Set<SecurityRoleEnum> globalAccessRoles);
}
