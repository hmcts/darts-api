package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUserIds;

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

    public SecurityGroupWithIdAndRoleAndUserIds mapToSecurityGroupWithCourthousesAndUsers(SecurityGroupEntity securityGroupEntity) {
        if (securityGroupEntity == null) {
            return null;
        }

        SecurityGroupWithIdAndRoleAndUserIds securityGroupWithIdAndRoleAndUserIds = new SecurityGroupWithIdAndRoleAndUserIds();

        securityGroupWithIdAndRoleAndUserIds.setName(securityGroupEntity.getGroupName());
        securityGroupWithIdAndRoleAndUserIds.setSecurityRoleId(securityGroupEntity.getSecurityRoleEntity().getId());
        securityGroupWithIdAndRoleAndUserIds.setId(securityGroupEntity.getId());
        securityGroupWithIdAndRoleAndUserIds.setGlobalAccess(securityGroupEntity.getGlobalAccess());
        securityGroupWithIdAndRoleAndUserIds.setDisplayState(securityGroupEntity.getDisplayState());
        securityGroupWithIdAndRoleAndUserIds.setDisplayName(securityGroupEntity.getDisplayName());
        securityGroupWithIdAndRoleAndUserIds.setDescription(securityGroupEntity.getDescription());

        Set<CourthouseEntity> courthouseEntities = securityGroupEntity.getCourthouseEntities();
        securityGroupWithIdAndRoleAndUserIds.setCourthouseIds(courthouseEntities
            .stream().map(CourthouseEntity::getId).sorted().collect(Collectors.toList()));
        Set<UserAccountEntity> users = securityGroupEntity.getUsers();
        securityGroupWithIdAndRoleAndUserIds.setUserIds(users.stream().map(UserAccountEntity::getId).sorted().collect(Collectors.toList()));

        return securityGroupWithIdAndRoleAndUserIds;
    }
}
