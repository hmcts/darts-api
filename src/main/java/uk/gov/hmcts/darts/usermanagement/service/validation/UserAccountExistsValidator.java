package uk.gov.hmcts.darts.usermanagement.service.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.validation.Validator;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;

@Component("userAccountExistsValidator")
@RequiredArgsConstructor
public class UserAccountExistsValidator implements Validator<Integer> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void validate(Integer userId) {
        if (!userAccountRepository.existsById(userId)) {
            throw new DartsApiException(
                UserManagementError.USER_NOT_FOUND,
                String.format("User id %d not found", userId));
        }
    }
}
