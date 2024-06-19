package uk.gov.hmcts.darts.usermanagement.validator;

import lombok.RequiredArgsConstructor;
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

        if (request.getPayload().getActive()) {
            Optional<UserAccountEntity> fndUser = userAccountRepository.findById(request.getId());

            if (fndUser.isPresent()) {
                UserAccountEntity userAccountEntity = fndUser.get();
                String emailAddress = userAccountEntity.getEmailAddress();
                String fullName = userAccountEntity.getUserFullName();

                if (emailAddress == null || emailAddress.isEmpty() || fullName == null || fullName.isEmpty()) {
                    throw new DartsApiException(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION);
                }
            }
        }
    }
}