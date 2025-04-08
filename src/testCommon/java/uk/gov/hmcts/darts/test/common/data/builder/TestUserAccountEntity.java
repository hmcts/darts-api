package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.Set;

// TestClassWithoutTestCases suppression: This is not a test class.
// ConstructorCallsOverridableMethod suppression: If this proves to be a demonstrable problem, we can change the object creation approach. For now, it is fine.
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestUserAccountEntity extends UserAccountEntity implements DbInsertable<UserAccountEntity> {

    @lombok.Builder
    public TestUserAccountEntity(
        Integer id,
        String dmObjectId,
        String userName,
        String userFullName,
        String emailAddress,
        String userDescription,
        Boolean active,
        OffsetDateTime lastLoginTime,
        String accountGuid,
        Boolean isSystemUser,
        Set<SecurityGroupEntity> securityGroupEntities,
        String userOsName,
        String userLdapDomainName,
        String userGlobalUniqueId,
        String userLoginName,
        String userLoginDomain,
        Short userState,
        OffsetDateTime createdDateTime,
        Integer createdById,
        OffsetDateTime lastModifiedDateTime,
        Integer lastModifiedById
    ) {
        setId(id);
        setDmObjectId(dmObjectId);
        setUserName(userName);
        setUserFullName(userFullName);
        setEmailAddress(emailAddress);
        setUserDescription(userDescription);
        setActive(active);
        setLastLoginTime(lastLoginTime);
        setAccountGuid(accountGuid);
        setIsSystemUser(isSystemUser);
        setSecurityGroupEntities(securityGroupEntities);
        setUserOsName(userOsName);
        setUserLdapDomainName(userLdapDomainName);
        setUserGlobalUniqueId(userGlobalUniqueId);
        setUserLoginName(userLoginName);
        setUserLoginDomain(userLoginDomain);
        setUserState(userState);
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(lastModifiedById);
    }

    @Override
    public UserAccountEntity getEntity() {
        try {
            UserAccountEntity userAccountEntity = new UserAccountEntity();
            BeanUtils.copyProperties(userAccountEntity, this);
            userAccountEntity.setActive(this.isActive()); // Needed as BeanUtils gets confused with the getter/setting naming on this Boolean field
            return userAccountEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestUserAccountEntityBuilderRetrieve
        implements BuilderHolder<TestUserAccountEntity, TestUserAccountEntity.TestUserAccountEntityBuilder> {

        private TestUserAccountEntity.TestUserAccountEntityBuilder builder = TestUserAccountEntity.builder();

        @Override
        public TestUserAccountEntity build() {
            return builder.build();
        }

        @Override
        public TestUserAccountEntity.TestUserAccountEntityBuilder getBuilder() {
            return builder;
        }
    }

}