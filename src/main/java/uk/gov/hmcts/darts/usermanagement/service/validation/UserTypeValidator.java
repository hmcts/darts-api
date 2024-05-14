package uk.gov.hmcts.darts.usermanagement.service.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;

import java.util.Optional;

@Component("userTypeValidator")
@RequiredArgsConstructor
public class UserTypeValidator implements Validator<Integer> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void validate(Integer userId) {
        Optional<UserAccountEntity> accountEntity = userAccountRepository.findById(userId);

        if (accountEntity.isPresent() && accountEntity.get().getIsSystemUser()) {
            throw new DartsApiException(
                UserManagementError.USER_NOT_FOUND);
        }
    }
}