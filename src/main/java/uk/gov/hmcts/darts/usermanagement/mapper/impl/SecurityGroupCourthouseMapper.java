package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityGroupCourthouseMapper {

    private final SecurityGroupMapper securityGroupMapper;

    public SecurityGroupWithIdAndRole mapToSecurityGroupWithIdAndRoleWithCourthouse(SecurityGroupEntity securityGroupEntity) {
        SecurityGroupWithIdAndRole securityGroupWithIdAndRole = securityGroupMapper.mapToSecurityGroupWithIdAndRole(securityGroupEntity);
        securityGroupWithIdAndRole.setSecurityRoleId(securityGroupEntity.getSecurityRoleEntity().getId());
        List<Integer> courthouseIds = securityGroupEntity.getCourthouseEntities().stream().map(CourthouseEntity::getId).sorted().toList();
        securityGroupWithIdAndRole.setCourthouseIds(courthouseIds);
        return securityGroupWithIdAndRole;
    }

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
        Set<UserAccountEntity> users = securityGroupEntity.getUsers();
        securityGroupWithIdAndRoleAndUsers.setUserIds(users.stream().map(UserAccountEntity::getId).sorted().collect(Collectors.toList()));

        return securityGroupWithIdAndRoleAndUsers;
    }
}
