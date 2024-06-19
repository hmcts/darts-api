package uk.gov.hmcts.darts.usermanagement.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActivateValidatorTest {

    @InjectMocks
    UserActivateValidator userAuthoriseValidator;

    @Mock
    UserAccountRepository userAccountRepository;

    @Test
    void testValidateActivateSuccess() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserFullName("fullname");
        entity.setEmailAddress("test@hmcts.net");

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        userAuthoriseValidator.validate(new IdRequest<>(patch, userId));
    }

    @Test
    void testValidateActivateFailureNoEmail() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();
        entity.setEmailAddress("test@hmcts.net");

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        DartsApiException exception = assertThrows(DartsApiException.class, () -> userAuthoriseValidator.validate(new IdRequest<>(patch, userId)));
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION, exception.getError());
    }

    @Test
    void testValidateActivateFailureNoFullName() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserFullName("fullname");

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        DartsApiException exception = assertThrows(DartsApiException.class, () -> userAuthoriseValidator.validate(new IdRequest<>(patch, userId)));
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION, exception.getError());
    }

    @Test
    void testValidateActivateFailureNoFullNameAndEmailAddress() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        DartsApiException exception = assertThrows(DartsApiException.class, () -> userAuthoriseValidator.validate(new IdRequest<>(patch, userId)));
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION, exception.getError());
    }

    @Test
    void testValidateDeactivateNoFailureOnNoFullNameAndNoEmailAddress() {
        Integer userId = 200;

        UserPatch patch = new UserPatch();
        patch.setActive(false);

        userAuthoriseValidator.validate(new IdRequest<>(patch, userId));
    }
}