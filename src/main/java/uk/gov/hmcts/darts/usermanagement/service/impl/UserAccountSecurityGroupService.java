package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountSecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;

    public void unassignUserFromGroupsTheyArePartOf(UserAccountEntity userAccount) {
        Set<SecurityGroupEntity> groupEntities = new LinkedHashSet<>(userAccount.getSecurityGroupEntities());
        userAccount.getSecurityGroupEntities().clear();

        for (SecurityGroupEntity groupEntity : groupEntities) {
            groupEntity.getUsers().remove(userAccount);
            securityGroupRepository.save(groupEntity);
        }
    }
}
