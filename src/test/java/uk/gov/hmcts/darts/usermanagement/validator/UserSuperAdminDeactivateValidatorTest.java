package uk.gov.hmcts.darts.usermanagement.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Arrays;

public class UserSuperAdminDeactivateValidatorTest {

    private UserSuperAdminDeactivateValidator userEnablementValidator;

    private UserIdentity userIdentity;

    public UserSuperAdminDeactivateValidatorTest() {
        userIdentity = Mockito.mock(UserIdentity.class);
        userEnablementValidator = new UserSuperAdminDeactivateValidator(userIdentity);
    }

    @Test
    public void testFailActivateTrue() {
        UserPatch patch = new UserPatch();
        patch.setActive(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    public void testFailActivateFalseSetDescription() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setDescription("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    public void testFailActivateFalseSetEmailAddress() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setEmailAddress("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    public void testFailActiveFalseSetFullName() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.setFullName("");

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    public void testFailActivateFalseSetGroupIds() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        patch.securityGroupIds(Arrays.asList(12));

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class,
                                                       () -> userEnablementValidator.validate(patch));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    public void testSuccessDeactivate() {
        UserPatch patch = new UserPatch();
        patch.setActive(false);

        Mockito.when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);

        userEnablementValidator.validate(patch);
    }
}