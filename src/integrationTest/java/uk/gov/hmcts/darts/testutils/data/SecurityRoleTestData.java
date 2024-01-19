package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class SecurityRoleTestData {

    public static SecurityRoleEntity securityRoleFor(SecurityRoleEnum role) {
        var securityRole = new SecurityRoleEntity();
        securityRole.setId(role.getId());
        return securityRole;
    }
}
