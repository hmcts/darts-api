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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateEmailValidationTest {
    private static final String NEW_EMAIL_ADDRESS = "new-email@hmcts.net";
    private static final String EXISTING_EMAIL_ADDRESS = "existing-email@hmcts.net";

    @Mock
    private UserAccountRepository userAccountRepository;
    private DuplicateEmailValidation duplicateEmailValidation;

    @BeforeEach
    void setUp() {
        duplicateEmailValidation = new DuplicateEmailValidation(userAccountRepository);
    }

    @Test
    void doesNotThrowExceptionIfEmailNotCurrentlyAssociatedWithActiveUser() {
       when(userAccountRepository.findByEmailAddressIgnoreCaseAndState(NEW_EMAIL_ADDRESS, 0))
           .thenReturn(Collections.emptyList());

       duplicateEmailValidation.validate(someUserWithEmail(NEW_EMAIL_ADDRESS));

       assertThatNoException().isThrownBy(() -> duplicateEmailValidation.validate(someUserWithEmail(NEW_EMAIL_ADDRESS)));
    }

    @Test
    void throwsExceptionIfEmailAlreadyAssociatedWithActiveUser() {
        when(userAccountRepository.findByEmailAddressIgnoreCaseAndState(EXISTING_EMAIL_ADDRESS, 0))
            .thenReturn(List.of(someUserAccountWithEmail(EXISTING_EMAIL_ADDRESS)));

        assertThatThrownBy(() -> duplicateEmailValidation.validate(someUserWithEmail(EXISTING_EMAIL_ADDRESS)))
            .isInstanceOf(DartsApiException.class);
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
