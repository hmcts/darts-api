package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityGroupCourthouseMapper {

    private final SecurityGroupMapper securityGroupMapper;

    public SecurityGroupWithIdAndRole mapToSecurityGroupWithIdAndRoleWithCourthouse(SecurityGroupEntity securityGroupEntity) {
        SecurityGroupWithIdAndRole securityGroupWithIdAndRole = securityGroupMapper.mapToSecurityGroupWithIdAndRole(securityGroupEntity);
        securityGroupWithIdAndRole.setSecurityRoleId(securityGroupEntity.getSecurityRoleEntity().getId());
        List<Integer> courthouseIds = securityGroupEntity.getCourthouseEntities().stream().map(CourthouseEntity::getId).toList();
        securityGroupWithIdAndRole.setCourthouseIds(courthouseIds);
        return securityGroupWithIdAndRole;
    }
}
