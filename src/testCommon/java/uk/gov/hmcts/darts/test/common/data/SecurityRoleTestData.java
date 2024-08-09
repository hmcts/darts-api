package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import static org.apache.commons.lang3.RandomStringUtils.random;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class SecurityRoleTestData {

    public static SecurityRoleEntity createSecurityRoleFor(SecurityRoleEnum role) {
        var postfix = random(10, false, true);
        var securityRole = new SecurityRoleEntity();
        securityRole.setId(role.getId());
        securityRole.setRoleName("some-role-name-" + postfix);
        securityRole.setDisplayName("some-display-name-" + postfix);
        securityRole.setDisplayState(true);
        return securityRole;
    }
}
