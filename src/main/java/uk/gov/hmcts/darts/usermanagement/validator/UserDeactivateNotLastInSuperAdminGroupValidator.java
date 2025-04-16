package uk.gov.hmcts.darts.usermanagement.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserDeactivateNotLastInSuperAdminGroupValidator implements Validator<IdRequest<UserPatch>> {

    private final SecurityGroupRepository securityGroupRepository;

    @Override
    public void validate(IdRequest<UserPatch> userPatch) {
        if (!(userPatch.getPayload().getActive() != null && !userPatch.getPayload().getActive())) {
            return;
        }
        Optional<SecurityGroupEntity> securityGroupEntityLst = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
        Set<UserAccountEntity> accountEntities = securityGroupEntityLst.orElseThrow().getUsers();

        // if the super admin group has only 1 user and its the user we are deactivating fail
        if (accountEntities.size() != 1) {
            return;
        }
        Iterator<UserAccountEntity> entity = accountEntities.iterator();
        UserAccountEntity userAccountEntity = entity.next();
        if (userAccountEntity.getId().equals(userPatch.getId())) {
            throw new DartsApiException(AuthorisationError.UNABLE_TO_DEACTIVATE_USER);
        }
    }
}