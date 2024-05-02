package uk.gov.hmcts.darts.usermanagement.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserEnablementValidator implements Validator<UserPatch> {

    private final UserIdentity userIdentity;

    @Override
    public void validate(UserPatch userPatch) {

        // only allow super user to transition from false to true
        if (userPatch.getActive() != null && userPatch.getActive().equals(true)) {
            Set<SecurityRoleEnum> securityRoleEnum = new HashSet<>();
            securityRoleEnum.add(SecurityRoleEnum.SUPER_USER);

            if (!userIdentity.userHasGlobalAccess(securityRoleEnum)) {
                throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT);
            }
        }
    }
}