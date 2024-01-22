package uk.gov.hmcts.darts.authorisation.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.List;
import java.util.Set;

@UtilityClass
public class SecurityGroupUtil {

    public boolean matchesAtLeastOneSecurityGroup(Set<SecurityGroupEntity> securityGroupEntities, List<SecurityRoleEnum> securityRoles) {
        List<Integer> securityRoleIds = securityRoles.stream().map(SecurityRoleEnum::getId).toList();
        return securityGroupEntities.stream()
            .anyMatch(securityGroup -> securityRoleIds.contains(securityGroup.getSecurityRoleEntity().getId()));
    }

}
