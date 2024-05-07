package uk.gov.hmcts.darts.usermanagement.validator;

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
public class UserSuperAdminDeactivateValidator implements Validator<UserPatch> {

    private final UserIdentity userIdentity;

    @Override
    public void validate(UserPatch userPatch) {

        // only allow super user to transition from false to true
        if (userPatch.getActive() != null && userPatch.getActive().equals(true)) {
            throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT);
        } else {
            Set<SecurityRoleEnum> securityRoleEnum = new HashSet<>();
            securityRoleEnum.add(SecurityRoleEnum.SUPER_ADMIN);

            if (userIdentity.userHasGlobalAccess(securityRoleEnum)) {
                if (hasAnythingOtherThanEnablementStateChanged(userPatch)) {
                    throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_PAYLOAD_ENDPOINT);
                }
            }
        }
    }

    private boolean hasAnythingOtherThanEnablementStateChanged(UserPatch userPatch) {
        return userPatch.getDescription() != null
            || userPatch.getEmailAddress() != null
            || userPatch.getFullName() != null
            || userPatch.getSecurityGroupIds() != null && !userPatch.getSecurityGroupIds().isEmpty();
    }
}