package uk.gov.hmcts.darts.usermanagement.validator;

import lombok.RequiredArgsConstructor;
import org.apache.tika.utils.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserActivateValidator implements Validator<IdRequest<UserPatch, Integer>> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void validate(IdRequest<UserPatch, Integer> request) {
        if (!(request.getPayload() != null && Boolean.TRUE.equals(request.getPayload().getActive()))) {
            return;
        }

        Optional<UserAccountEntity> fndUser = userAccountRepository.findById(request.getId());

        if (!(fndUser.isPresent() && !fndUser.get().isActive())) {
            return;
        }
        UserAccountEntity userAccountEntity = fndUser.get();
        String emailAddress = userAccountEntity.getEmailAddress();

        if (StringUtils.isBlank(emailAddress)) {
            throw new DartsApiException(UserManagementError.USER_ACTIVATION_EMAIL_VIOLATION);
        }
    }
}