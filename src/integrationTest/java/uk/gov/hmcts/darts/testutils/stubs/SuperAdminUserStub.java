package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.test.common.SecurityRoleMatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@Component
@RequiredArgsConstructor
public class SuperAdminUserStub {

    private final UserAccountStub userAccountStub;

    public UserAccountEntity givenUserIsAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.createSuperAdminUser();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(any()))
            .thenReturn(true);

        return user;
    }

    public UserAccountEntity givenUserIsAuthorised(Authentication authentication) {
        var user = userAccountStub.createSuperAdminUser();

        setupUserAsAuthorised(authentication, user);

        return user;
    }

    public UserAccountEntity givenUserIsAuthorised(UserIdentity userIdentity, SecurityRoleEnum roleEnum) {
        var user = userAccountStub.createSuperAdminUser();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(Set.of(roleEnum)))
            .thenReturn(true);

        return user;
    }

    public UserAccountEntity givenUserIsAuthorised(Authentication authentication, SecurityRoleEnum roleEnum) {
        var user = userAccountStub.createSuperAdminUser();

        setupUserAsAuthorised(authentication, user);

        return user;
    }

    public UserAccountEntity givenUserIsAuthorisedButInactive(UserIdentity userIdentity) {
        var user = userAccountStub.createSuperAdminUserInactive();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(any()))
            .thenReturn(true);

        return user;
    }

    public UserAccountEntity givenUserIsAuthorisedButInactive(Authentication authentication) {
        var user = userAccountStub.createSuperAdminUserInactive();

        setupUserAsAuthorised(authentication, user);

        return user;
    }

    public UserAccountEntity givenSystemAdminIsAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.createSuperAdminUser();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(argThat(new SecurityRoleMatcher(SecurityRoleEnum.SUPER_ADMIN))))
            .thenReturn(true);
        Mockito.when(userIdentity.userHasGlobalAccess(argThat(new SecurityRoleMatcher(SecurityRoleEnum.SUPER_USER))))
            .thenReturn(true);

        return user;
    }

    public UserAccountEntity givenUserIsNotAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.getIntegrationTestUserAccountEntity();
        user.setSecurityGroupEntities(Collections.emptySet());

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(any()))
            .thenReturn(false);

        return user;
    }

    public UserAccountEntity givenUserIsNotAuthorised(Authentication authentication) {
        var user = userAccountStub.getIntegrationTestUserAccountEntity();
        user.setSecurityGroupEntities(Collections.emptySet());

        setupUserAsAuthorised(authentication, user);

        return user;
    }

    public UserAccountEntity givenSystemUserIsAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.createSuperUser();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(argThat(new SecurityRoleMatcher(SecurityRoleEnum.SUPER_USER))))
            .thenReturn(true);

        return user;
    }

    public void setupUserAsAuthorised(Authentication authentication, UserAccountEntity userAccountEntity) {
        Jwt jwt = Mockito.mock(Jwt.class);
        Map<String, Object> oidMap = new HashMap<>();
        oidMap.put("oid", userAccountEntity.getAccountGuid());
        oidMap.put("emails", List.of(userAccountEntity.getEmailAddress()));
        Mockito.when(jwt.getClaims()).thenReturn(oidMap);
        Mockito.when(authentication.getPrincipal()).thenReturn(jwt);
   }
}