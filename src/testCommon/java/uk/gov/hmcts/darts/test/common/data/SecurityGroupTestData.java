package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.SecurityRoleTestData.securityRoleFor;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class SecurityGroupTestData {

    public static SecurityGroupEntity minimalSecurityGroup(UserAccountEntity userAccountEntity) {
        var postfix = random(10);
        var securityGroup = new SecurityGroupEntity();
        securityGroup.setGroupName("some-group-name-" + postfix);
        securityGroup.setGlobalAccess(false);
        securityGroup.setDisplayState(true);
        securityGroup.setUseInterpreter(false);
        securityGroup.setDisplayName("Some Group Name " + postfix);
        securityGroup.setDescription("a-test-security-group");
        securityGroup.setCreatedBy(userAccountEntity);
        securityGroup.setLastModifiedBy(userAccountEntity);
        return securityGroup;
    }

    public static SecurityGroupEntity buildGroupForRole(SecurityRoleEnum role) {
        var securityGroupEntity = minimalSecurityGroup(minimalUserAccount());
        securityGroupEntity.setSecurityRoleEntity(securityRoleFor(role));
        return securityGroupEntity;
    }

    public static SecurityGroupEntity buildGroupForRoleAndCourthouse(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var securityGroupEntity = buildGroupForRole(role);
        securityGroupEntity.setCourthouseEntities(Set.of(courthouse));
        return securityGroupEntity;
    }
}
