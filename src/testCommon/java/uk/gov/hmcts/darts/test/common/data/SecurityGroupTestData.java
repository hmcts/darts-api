package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.SecurityRoleTestData.createSecurityRoleFor;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class SecurityGroupTestData {

    private SecurityGroupTestData() {

    }

    public static SecurityGroupEntity minimalSecurityGroup(UserAccountEntity userAccountEntity) {
        var postfix = random(10, false, true);
        var securityGroup = new SecurityGroupEntity();
        securityGroup.setGroupName("some-group-name-" + postfix);
        securityGroup.setGlobalAccess(false);
        securityGroup.setDisplayState(true);
        securityGroup.setUseInterpreter(false);
        securityGroup.setDisplayName("Some Group Name " + postfix);
        securityGroup.setDescription("a-test-security-group");
        securityGroup.setCreatedBy(userAccountEntity);
        securityGroup.setLastModifiedById(0);
        return securityGroup;
    }

    // Use with caution, when persisted it will overwrite values for subsequent tests
    @Deprecated
    public static SecurityGroupEntity createGroupForRole(SecurityRoleEnum role) {
        var securityGroupEntity = minimalSecurityGroup(minimalUserAccount());
        securityGroupEntity.setSecurityRoleEntity(createSecurityRoleFor(role));
        return securityGroupEntity;
    }

    // Use with caution, when persisted it will overwrite values for subsequent tests
    @Deprecated
    public static SecurityGroupEntity buildGroupForRoleAndCourthouse(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var securityGroupEntity = createGroupForRole(role);
        securityGroupEntity.setCourthouseEntities(Set.of(courthouse));
        return securityGroupEntity;
    }
}