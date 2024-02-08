package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Random;
import java.util.Set;

import static uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class UserAccountTestData {

    private static final Random RANDOM = new Random();

    public static UserAccountEntity minimalUserAccount() {
        var userAccount = new UserAccountEntity();
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        userAccount.setUserName("some-user-name-" + RANDOM.nextInt(1000, 9999));
        userAccount.setUserFullName("some-user-full-name");
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
