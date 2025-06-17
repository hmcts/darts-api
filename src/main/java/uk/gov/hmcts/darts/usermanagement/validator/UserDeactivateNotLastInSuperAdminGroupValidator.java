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
public class UserDeactivateNotLastInSuperAdminGroupValidator implements Validator<IdRequest<UserPatch, Integer>> {

    private final SecurityGroupRepository securityGroupRepository;

    @Override
    public void validate(IdRequest<UserPatch, Integer> userPatch) {
        if (!(userPatch.getPayload().getActive() != null && !userPatch.getPayload().getActive())) {
            return;
        }
        Optional<SecurityGroupEntity> securityGroupEntityLst = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
        Set<UserAccountEntity> accountEntities = securityGroupEntityLst.orElseThrow().getUsers();

        if (accountEntities.isEmpty()) {
            return;
        }
        // if the super admin group has 0 users and it's the user we are deactivating fail
        Iterator<UserAccountEntity> entity = accountEntities.iterator();
        UserAccountEntity userAccountEntity = entity.next();
        if (userAccountEntity.getId().equals(userPatch.getId())) {
            throw new DartsApiException(AuthorisationError.UNABLE_TO_DEACTIVATE_USER);
        }
    }
}