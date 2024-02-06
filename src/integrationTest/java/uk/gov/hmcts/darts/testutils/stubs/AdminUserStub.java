package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

@Component
@RequiredArgsConstructor
public class AdminUserStub {

    private final UserAccountStub userAccountStub;

    public UserAccountEntity givenUserIsAuthorised(UserIdentity userIdentity) {
        var user = userAccountStub.createAdminUser();

        Mockito.when(userIdentity.getUserAccount())
              .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(any()))
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

}
