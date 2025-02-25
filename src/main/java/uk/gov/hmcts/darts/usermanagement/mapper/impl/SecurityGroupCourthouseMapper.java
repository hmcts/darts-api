package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityGroupCourthouseMapper {

    public SecurityGroupWithIdAndRoleAndUsers mapToSecurityGroupWithCourthousesAndUsers(SecurityGroupEntity securityGroupEntity) {
        if (securityGroupEntity == null) {
            return null;
        }

        SecurityGroupWithIdAndRoleAndUsers securityGroupWithIdAndRoleAndUsers = new SecurityGroupWithIdAndRoleAndUsers();

        securityGroupWithIdAndRoleAndUsers.setName(securityGroupEntity.getGroupName());
        securityGroupWithIdAndRoleAndUsers.setSecurityRoleId(securityGroupEntity.getSecurityRoleEntity().getId());
        securityGroupWithIdAndRoleAndUsers.setId(securityGroupEntity.getId());
        securityGroupWithIdAndRoleAndUsers.setGlobalAccess(securityGroupEntity.getGlobalAccess());
        securityGroupWithIdAndRoleAndUsers.setDisplayState(securityGroupEntity.getDisplayState());
        securityGroupWithIdAndRoleAndUsers.setDisplayName(securityGroupEntity.getDisplayName());
        securityGroupWithIdAndRoleAndUsers.setDescription(securityGroupEntity.getDescription());

        Set<CourthouseEntity> courthouseEntities = securityGroupEntity.getCourthouseEntities();
        securityGroupWithIdAndRoleAndUsers.setCourthouseIds(courthouseEntities
                                                                .stream().map(CourthouseEntity::getId).sorted().collect(Collectors.toList()));

        List<UserAccountEntity> nonSystemUsers = Optional.ofNullable(securityGroupEntity.getUsers())
            .map(usrs -> usrs.stream()
                .filter(user -> !Boolean.TRUE.equals(user.getIsSystemUser()))
                .toList())
            .orElse(Collections.emptyList());

        securityGroupWithIdAndRoleAndUsers.setUserIds(nonSystemUsers.stream().map(UserAccountEntity::getId).sorted().toList());

        return securityGroupWithIdAndRoleAndUsers;
    }
}
