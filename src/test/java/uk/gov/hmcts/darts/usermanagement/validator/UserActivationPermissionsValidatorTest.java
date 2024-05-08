package uk.gov.hmcts.darts.usermanagement.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Arrays;

class UserActivationPermissionsValidatorTest {

    private final AuthorisedUserPermissionsValidator userEnablementValidator;

    private final UserIdentity userIdentity;

    public UserActivationPermissionsValidatorTest() {
        userIdentity = Mockito.mock(UserIdentity.class);
        userEnablementValidator = new AuthorisedUserPermissionsValidator(userIdentity);
    }

    @Test
    void testSuperUserActivateTrueFailure() {
        UserPatch patch = new UserPatch();
        patch.setActive(true);

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(false);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    void testSuperUserActivateFalseSetDescriptionFailure() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setDescription("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    void testSuperUserActivateFalseSetEmailAddressFailure() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setEmailAddress("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    void testSuperUserActiveFalseSetFullNameFailure() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setFullName("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    void testSuperUserActivateFalseSetGroupIdsSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.securityGroupIds(Arrays.asList(12));

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    void testSuperUserDeactivateSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        userEnablementValidator.validate(patch);
    }

    @Test
    void testSuperAdminActivateFalseSetDescriptionSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setDescription("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(false, true);

        userEnablementValidator.validate(patch);
    }

    @Test
    void testSuperAdminActivateFalseSetEmailAddressSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setEmailAddress("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(false, true);

        userEnablementValidator.validate(patch);
    }

    @Test
    void testSuperAdminActiveFalseSetFullNameSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setFullName("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(false, true);

        userEnablementValidator.validate(patch);
    }

    @Test
    void testSuperAdminActivateFalseSetGroupIdsSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.securityGroupIds(Arrays.asList(12));

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(false, true);

        userEnablementValidator.validate(patch);
    }

    @Test
    void testSuperAdminActivateSuccess() {
        UserPatch patch = new UserPatch();
        patch.setActive(true);

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        userEnablementValidator.validate(patch);
    }
}