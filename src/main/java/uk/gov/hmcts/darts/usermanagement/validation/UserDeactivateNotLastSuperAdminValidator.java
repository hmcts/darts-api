package uk.gov.hmcts.darts.usermanagement.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.validation.UserQueryRequest;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserDeactivateNotLastSuperAdminValidator implements Validator<UserQueryRequest<UserPatch>> {

    private final SecurityGroupRepository securityGroupRepository;

    @Override
    public void validate(UserQueryRequest<UserPatch> userPatch) {
        if (userPatch.getPayload().getActive() != null && !userPatch.getPayload().getActive()) {
            Optional<SecurityGroupEntity> securityGroupEntityLst = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
            Set<UserAccountEntity> accountEntities = securityGroupEntityLst.get().getUsers();

            // if the super admin group has only 1 user and its the user we are deactivating fail
            if (securityGroupEntityLst.isPresent() && accountEntities.size() == 1) {
                Iterator<UserAccountEntity> entity = accountEntities.iterator();
                UserAccountEntity userAccountEntity = entity.next();
                if (userAccountEntity.getId().equals(userPatch.getUserId())) {
                    throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_ENDPOINT);
                }
            }
        }
    }
}