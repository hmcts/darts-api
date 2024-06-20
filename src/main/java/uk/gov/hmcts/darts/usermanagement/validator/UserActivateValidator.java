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
public class UserActivateValidator implements Validator<IdRequest<UserPatch>> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void validate(IdRequest<UserPatch> request) {

        if (request.getPayload() != null && Boolean.TRUE.equals(request.getPayload().getActive())) {
            Optional<UserAccountEntity> fndUser = userAccountRepository.findById(request.getId());

            if (fndUser.isPresent() && !fndUser.get().isActive()) {
                UserAccountEntity userAccountEntity = fndUser.get();
                String emailAddress = userAccountEntity.getEmailAddress();
                String fullName = userAccountEntity.getUserFullName();

                if (StringUtils.isBlank(emailAddress) || StringUtils.isBlank(fullName)) {
                    throw new DartsApiException(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION);
                }
            }
        }
    }
}