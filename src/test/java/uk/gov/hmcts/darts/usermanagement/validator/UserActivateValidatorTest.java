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
    void validate_shouldSucceed_whenDeactivatedUserExistsWithAnEmailAddress_andWeAttemptToActivateTheUser() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();
        entity.setEmailAddress("test@hmcts.net");
        entity.setActive(false);

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        userAuthoriseValidator.validate(new IdRequest<>(patch, userId));
    }

    @Test
    void validate_shouldThrowException_whenDeactivatedUserExistsWithNoEmailAddress_andWeAttemptToActivateTheUser() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();
        entity.setActive(false);

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        DartsApiException exception = assertThrows(DartsApiException.class, () -> userAuthoriseValidator.validate(new IdRequest<>(patch, userId)));
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_EMAIL_VIOLATION, exception.getError());
    }

    @Test
    void validate_shouldSucceed_whenActivatedUserExists_andWeAttemptToActivateTheUser() {
        Integer userId = 200;

        UserAccountEntity entity = new UserAccountEntity();
        entity.setActive(true);

        UserPatch patch = new UserPatch();
        patch.setActive(true);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(entity));
        userAuthoriseValidator.validate(new IdRequest<>(patch, userId));
    }

    @Test
    void validate_shouldSucceed_whenADeactivateRequestIsSent() {
        Integer userId = 200;

        UserPatch patch = new UserPatch();
        patch.setActive(false);

        userAuthoriseValidator.validate(new IdRequest<>(patch, userId));
    }
}