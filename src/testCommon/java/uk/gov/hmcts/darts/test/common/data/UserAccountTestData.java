package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class UserAccountTestData {

    public static UserAccountEntity minimalUserAccount() {
        var postfix = random(10, false, true);
        var userAccount = new UserAccountEntity();
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        userAccount.setUserName("some-user-name-" + postfix);
        userAccount.setUserFullName("some-user-full-name");
        UserAccountEntity systemUser = getSystemUser();
        userAccount.setCreatedBy(systemUser);
        userAccount.setCreatedDateTime(OffsetDateTime.now());
        userAccount.setLastModifiedBy(systemUser);
        userAccount.setLastModifiedDateTime(OffsetDateTime.now());
        return userAccount;
    }

    public static UserAccountEntity getSystemUser() {
        var userAccount = new UserAccountEntity();
        userAccount.setId(0);
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
