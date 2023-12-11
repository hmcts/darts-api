package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.USER_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class UserAccountExistsValidatorTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    private UserAccountExistsValidator userAccountExistsValidator;

    @BeforeEach
    void setUp() {
        userAccountExistsValidator = new UserAccountExistsValidator(userAccountRepository);
    }

    @Test
    void throwsIfUserNotFound() {
        when(userAccountRepository.existsById(1)).thenReturn(false);

        assertThatThrownBy(() -> userAccountExistsValidator.validate(1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", USER_NOT_FOUND)
            .hasFieldOrPropertyWithValue("detail", "User id 1 not found");
    }

    @Test
    void doesntThrowIfUserPresent() {
        when(userAccountRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() -> userAccountExistsValidator.validate(1));
    }
}
