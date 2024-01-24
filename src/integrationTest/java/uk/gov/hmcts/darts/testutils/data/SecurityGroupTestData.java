package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

import static uk.gov.hmcts.darts.testutils.data.SecurityRoleTestData.securityRoleFor;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class SecurityGroupTestData {

    public static SecurityGroupEntity minimalSecurityGroup() {
        var securityGroup = new SecurityGroupEntity();
        securityGroup.setGroupName("some-group-name");
        securityGroup.setGlobalAccess(false);
        securityGroup.setDisplayState(true);
        securityGroup.setUseInterpreter(false);
        securityGroup.setGroupDisplayName("Some Group Name");
        securityGroup.setDescription("a-test-security-group");
        return securityGroup;
    }

    public static SecurityGroupEntity buildGroupForRoleAndCourthouse(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var securityGroupEntity = minimalSecurityGroup();
        securityGroupEntity.setSecurityRoleEntity(securityRoleFor(role));
        securityGroupEntity.setCourthouseEntities(Set.of(courthouse));
        return securityGroupEntity;
    }
}
