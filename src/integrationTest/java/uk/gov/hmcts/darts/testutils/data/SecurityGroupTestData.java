package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Random;
import java.util.Set;

import static uk.gov.hmcts.darts.testutils.data.SecurityRoleTestData.securityRoleFor;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class SecurityGroupTestData {

    private static final Random RANDOM = new Random();

    public static SecurityGroupEntity minimalSecurityGroup() {
        int postfix = RANDOM.nextInt(1000, 9999);
        var securityGroup = new SecurityGroupEntity();
        securityGroup.setGroupName("some-group-name-" + postfix);
        securityGroup.setGlobalAccess(false);
        securityGroup.setDisplayState(true);
        securityGroup.setUseInterpreter(false);
        securityGroup.setDisplayName("Some Group Name " + postfix);
        securityGroup.setDescription("a-test-security-group");
        return securityGroup;
    }

    public static SecurityGroupEntity buildGroupForRole(SecurityRoleEnum role) {
        var securityGroupEntity = minimalSecurityGroup();
        securityGroupEntity.setSecurityRoleEntity(securityRoleFor(role));
        return securityGroupEntity;
    }

    public static SecurityGroupEntity buildGroupForRoleAndCourthouse(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var securityGroupEntity = buildGroupForRole(role);
        securityGroupEntity.setCourthouseEntities(Set.of(courthouse));
        return securityGroupEntity;
    }
}
