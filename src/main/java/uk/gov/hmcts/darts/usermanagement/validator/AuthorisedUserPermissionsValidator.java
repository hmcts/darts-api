package uk.gov.hmcts.darts.usermanagement.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthorisedUserPermissionsValidator implements Validator<UserPatch> {

    private final UserIdentity userIdentity;

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")//TODO - refactor to reduce complexity when this is next edited
    public void validate(UserPatch userPatch) {
        Set<SecurityRoleEnum> superUserRole = Set.of(SecurityRoleEnum.SUPER_USER);
        Set<SecurityRoleEnum> superAdminRole = Set.of(SecurityRoleEnum.SUPER_ADMIN);
        if (!userIdentity.userHasGlobalAccess(superAdminRole)) {
            if (userPatch.getActive() == null || userPatch.getActive().equals(false)
                && userIdentity.userHasGlobalAccess(superUserRole)) {
                if (hasAnythingOtherThanActivationStateChanged(userPatch)) {
                    throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_TO_USE_PAYLOAD_CONTENT);
                }
            } else if (userPatch.getActive() != null && userPatch.getActive().equals(true)) {
                throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_TO_ACTIVATE_USER);
            }
        }
    }

    private boolean hasAnythingOtherThanActivationStateChanged(UserPatch userPatch) {
        return userPatch.getDescription() != null
            || userPatch.getEmailAddress() != null
            || userPatch.getFullName() != null
            || userPatch.getSecurityGroupIds() != null && !userPatch.getSecurityGroupIds().isEmpty();
    }
}