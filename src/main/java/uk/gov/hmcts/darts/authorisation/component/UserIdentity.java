package uk.gov.hmcts.darts.authorisation.component;

import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserIdentity {

    UserAccountEntity getUserAccount();

    UserAccountEntity getUserAccount(Jwt jwt);

    Optional<Integer> getUserIdFromJwt();

    boolean userHasGlobalAccess(Set<SecurityRoleEnum> globalAccessRolest);

    List<Integer> getListOfCourthouseIdsUserHasAccessTo();

    Optional<UserAccountEntity> getUserAccountOptional(Jwt jwt);

    Jwt getJwt();
}