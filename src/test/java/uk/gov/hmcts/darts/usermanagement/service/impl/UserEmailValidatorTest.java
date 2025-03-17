package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserEmailValidator;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.DUPLICATE_EMAIL;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.INVALID_EMAIL_FORMAT;

@ExtendWith(MockitoExtension.class)
class UserEmailValidatorTest {
    private static final String NEW_EMAIL_ADDRESS = "new-email@hmcts.net";
    private static final String EXISTING_EMAIL_ADDRESS = "existing-email@hmcts.net";
    private static final String EMAIL_ADDRESS_NO_DOMAIN = "bad-email@";
    private static final String EMAIL_ADDRESS_NO_USER = "@hmcts.net";
    @Mock
    private UserAccountRepository userAccountRepository;
    private UserEmailValidator userEmailValidator;

    @BeforeEach
    void setUp() {
        userEmailValidator = new UserEmailValidator(userAccountRepository);
    }

    @Test
    void doesNotThrowExceptionIfEmailNotCurrentlyAssociatedWithActiveUser() {
        when(userAccountRepository.findByEmailAddressIgnoreCaseAndActive(NEW_EMAIL_ADDRESS, true))
            .thenReturn(Collections.emptyList());

        userEmailValidator.validate(someUserWithEmail(NEW_EMAIL_ADDRESS));

        assertThatNoException().isThrownBy(() -> userEmailValidator.validate(someUserWithEmail(NEW_EMAIL_ADDRESS)));
    }

    @Test
    void throwsExceptionIfEmailAlreadyAssociatedWithActiveUser() {
        when(userAccountRepository.findByEmailAddressIgnoreCaseAndActive(EXISTING_EMAIL_ADDRESS, true))
            .thenReturn(List.of(someUserAccountWithEmail(EXISTING_EMAIL_ADDRESS)));

        User user = someUserWithEmail(EXISTING_EMAIL_ADDRESS);
        assertThatThrownBy(() -> userEmailValidator.validate(user))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", DUPLICATE_EMAIL)
            .hasFieldOrPropertyWithValue("detail", String.format("User with email %s already exists", EXISTING_EMAIL_ADDRESS));
    }

    @Test
    void throwsExceptionIfEmailAddressHasNoDomain() {

        User user = someUserWithEmail(EMAIL_ADDRESS_NO_DOMAIN);
        assertThatThrownBy(() -> userEmailValidator.validate(user))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", INVALID_EMAIL_FORMAT)
            .hasFieldOrPropertyWithValue("detail", String.format("Invalid email format {%s}", EMAIL_ADDRESS_NO_DOMAIN));
    }

    @Test
    void throwsExceptionIfEmailAddressHasNoUser() {

        User user = someUserWithEmail(EMAIL_ADDRESS_NO_USER);
        assertThatThrownBy(() -> userEmailValidator.validate(user))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", INVALID_EMAIL_FORMAT)
            .hasFieldOrPropertyWithValue("detail", String.format("Invalid email format {%s}", EMAIL_ADDRESS_NO_USER));
    }

    private UserAccountEntity someUserAccountWithEmail(String email) {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setEmailAddress(email);
        return userAccountEntity;
    }

    private User someUserWithEmail(String email) {
        User user = new User();
        user.setEmailAddress(email);
        return user;
    }

}
