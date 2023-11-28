package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.User;

@Component
@RequiredArgsConstructor
public class DuplicateEmailValidation implements UserCreationValidation {

    private final UserAccountRepository userAccountRepository;
    @Override
    public void validate(User user) {
        userAccountRepository.findByEmailAddressIgnoreCaseAndState(user.getEmailAddress(), 0)
            .ifPresent(existingUser -> {
                throw new DartsApiException(
                    UserManagementError.DUPLICATE_EMAIL,
                    String.format("User with email %s already exists", user.getEmailAddress())
                );
            });
    }
}
