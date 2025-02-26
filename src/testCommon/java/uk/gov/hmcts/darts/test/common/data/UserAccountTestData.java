package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;

public final class UserAccountTestData {

    private UserAccountTestData() {

    }

    public static UserAccountEntity minimalUserAccount() {
        var userAccount = new UserAccountEntity();
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        userAccount.setUserFullName("some-user-full-name");
        userAccount.setLastModifiedById(0);
        return userAccount;
    }

    public static UserAccountEntity buildUserWithRoleFor(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var securityGroupEntity = buildGroupForRoleAndCourthouse(role, courthouse);
        var userAccount = minimalUserAccount();
        userAccount.setEmailAddress("some-" + role.name() + "@some-org.com");
        userAccount.setSecurityGroupEntities(Set.of(securityGroupEntity));
        return userAccount;
    }
}