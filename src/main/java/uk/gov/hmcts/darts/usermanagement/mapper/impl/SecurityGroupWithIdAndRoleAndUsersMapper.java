package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityGroupWithIdAndRoleAndUsersMapper {

    private final SecurityGroupMapper securityGroupMapper;

    public SecurityGroupWithIdAndRoleAndUsers mapToSecurityGroupWithIdAndRoleAndUsers(SecurityGroupEntity securityGroupEntity) {
        SecurityGroupWithIdAndRoleAndUsers securityGroupWithIdAndRoleAndUsers = securityGroupMapper.mapToSecurityGroupWithIdAndRoleAndUsers(
            securityGroupEntity);
        securityGroupWithIdAndRoleAndUsers.setSecurityRoleId(securityGroupEntity.getSecurityRoleEntity().getId());
        securityGroupWithIdAndRoleAndUsers.setCourthouseIds(
            securityGroupEntity.getCourthouseEntities().stream().map(CourthouseEntity::getId).sorted().toList());
        List<UserAccountEntity> nonSystemUsers = securityGroupEntity.getUsers().stream().filter(user -> !user.getIsSystemUser()).toList();
        securityGroupWithIdAndRoleAndUsers.setUserIds(nonSystemUsers.stream().map(UserAccountEntity::getId).sorted().toList());

        return securityGroupWithIdAndRoleAndUsers;
    }

}
