package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.test.common.data.builder.TestUserAccountEntity;

import java.time.OffsetDateTime;
import java.util.Set;

import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;

public final class UserAccountTestData
    implements Persistable<TestUserAccountEntity.TestUserAccountEntityBuilderRetrieve, UserAccountEntity,
    TestUserAccountEntity.TestUserAccountEntityBuilder> {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Override
    public UserAccountEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestUserAccountEntity.TestUserAccountEntityBuilderRetrieve someMinimalBuilderHolder() {
        var builder = new TestUserAccountEntity.TestUserAccountEntityBuilderRetrieve();

        builder.getBuilder()
            .createdDateTime(NOW)
            .lastModifiedDateTime(NOW)
            .isSystemUser(false)
            .active(true)
            .userFullName("Some User Full Name");

        return builder;
    }

    @Override
    public TestUserAccountEntity.TestUserAccountEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    // Deprecated, please use the methods provided by the Persistable interface
    @Deprecated
    public static UserAccountEntity minimalUserAccount() {
        var userAccount = new UserAccountEntity();
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        userAccount.setUserFullName("some-user-full-name");
        userAccount.setLastModifiedById(0);
        return userAccount;
    }

    // Deprecated, please use the methods provided by the Persistable interface
    @Deprecated
    public static UserAccountEntity buildUserWithRoleFor(SecurityRoleEnum role, CourthouseEntity courthouse) {
        var securityGroupEntity = buildGroupForRoleAndCourthouse(role, courthouse);
        var userAccount = minimalUserAccount();
        userAccount.setEmailAddress("some-" + role.name() + "@some-org.com");
        userAccount.setSecurityGroupEntities(Set.of(securityGroupEntity));
        return userAccount;
    }

}
