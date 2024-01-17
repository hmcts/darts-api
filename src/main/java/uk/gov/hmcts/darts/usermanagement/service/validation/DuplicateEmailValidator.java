package uk.gov.hmcts.darts.usermanagement.service.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.User;

@Component("duplicateEmailValidator")
@RequiredArgsConstructor
public class DuplicateEmailValidator implements Validator<User> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void validate(User user) {
        userAccountRepository.findByEmailAddressIgnoreCaseAndActive(user.getEmailAddress(), true)
            .stream().findFirst()
            .ifPresent(existingUser -> {
                throw new DartsApiException(
                    UserManagementError.DUPLICATE_EMAIL,
                    String.format("User with email %s already exists", existingUser.getEmailAddress())
                );
            });
    }
}
