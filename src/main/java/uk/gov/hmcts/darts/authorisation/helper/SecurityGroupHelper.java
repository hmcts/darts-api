package uk.gov.hmcts.darts.authorisation.helper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.List;
import java.util.Set;

@Component
public class SecurityGroupHelper {

    public boolean matchesAtLeastOneGlobalSecurityGroup(Set<SecurityGroupEntity> securityGroupEntities, List<SecurityRoleEnum> globalSecurityRoles) {
        List<Integer> globalSecurityRoleIds = globalSecurityRoles.stream().map(SecurityRoleEnum::getId).toList();
        return securityGroupEntities.stream()
            .filter(securityGroup -> securityGroup.getGlobalAccess())
            .anyMatch(securityGroup -> globalSecurityRoleIds.contains(securityGroup.getSecurityRoleEntity().getId()));

    }

}
