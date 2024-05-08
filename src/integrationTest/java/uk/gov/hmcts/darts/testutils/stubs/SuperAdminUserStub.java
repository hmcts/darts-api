package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Collections;
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
        Mockito.when(userIdentity.userHasGlobalAccess(argThat(new SecurityRoleMatcher(SecurityRoleEnum.SUPER_ADMIN))))
            .thenReturn(true);

        return user;
    }

    public UserAccountEntity givenSystemAdminIsAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.createSuperAdminUser();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(argThat(new SecurityRoleMatcher(SecurityRoleEnum.SUPER_ADMIN))))
            .thenReturn(true);

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

    public UserAccountEntity givenUserIsNotAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.getIntegrationTestUserAccountEntity();
        user.setSecurityGroupEntities(Collections.emptySet());

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(any()))
            .thenReturn(false);

        return user;
    }

    public class SecurityRoleMatcher implements ArgumentMatcher<Set<SecurityRoleEnum>> {

        private final SecurityRoleEnum assertRole;

        SecurityRoleMatcher(SecurityRoleEnum assertRole) {
            this.assertRole = assertRole;
        }

        @Override
        public boolean matches(Set<SecurityRoleEnum> roleSet) {
            return roleSet.stream().anyMatch(e -> e == assertRole);
        }
    }
}