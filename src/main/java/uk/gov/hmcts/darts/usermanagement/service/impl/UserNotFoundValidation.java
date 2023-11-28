package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserNotFoundValidation implements UserModifyValidation {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void validate(UserPatch validatable, Integer userId) {
        userAccountRepository.findById(userId)
            .orElseThrow(() -> new DartsApiException(
                UserManagementError.USER_NOT_FOUND,
                String.format("User id %d not found", userId)
            ));
    }
}
