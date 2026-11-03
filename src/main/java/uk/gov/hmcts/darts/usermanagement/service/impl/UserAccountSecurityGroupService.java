package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class UserAccountSecurityGroupService {

    public void unassignUserFromGroupsTheyArePartOf(UserAccountEntity userAccount) {
        Set<SecurityGroupEntity> groupEntities = new LinkedHashSet<>(userAccount.getSecurityGroupEntities());
        userAccount.getSecurityGroupEntities().clear();

        for (SecurityGroupEntity groupEntity : groupEntities) {
            if (Hibernate.isInitialized(groupEntity.getUsers())) {
                groupEntity.getUsers().remove(userAccount);
            }
        }
    }
}
