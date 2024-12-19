package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Deprecated
public class UserAccountStubComposable {
    private static final int SYSTEM_USER_ID = 0;
    public static final String INTEGRATION_TEST_USER_EMAIL = "integrationtest.user@example.com";
    public static final String SEPARATE_TEST_USER_EMAIL = "separateintegrationtest.user@example.com";

    private final UserAccountRepository userAccountRepository;
    private static final OffsetDateTime LAST_LOGIN_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime CREATED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

    private final SecurityGroupRepository securityGroupRepository;

    private final DartsPersistence dartsPersistence;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(CourthouseStubComposable courthouseStubComposable, String courthouse) {
        return createAuthorisedIntegrationTestUser(courthouseStubComposable.createCourthouseUnlessExists(courthouse));
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(CourthouseEntity... courthouseEntities) {
        return createReusableAuthorisedIntegrationTestUser(true, courthouseEntities);
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(boolean reuse, CourthouseEntity... courthouseEntities) {
        return createReusableAuthorisedIntegrationTestUser(reuse, courthouseEntities);
    }

    public UserAccountEntity getSystemUserAccountEntity() {

        Optional<UserAccountEntity> userAccountEntityOptional = userAccountRepository.findById(SYSTEM_USER_ID);

        if (userAccountEntityOptional.isPresent()) {
            return userAccountEntityOptional.get();
        } else {
            var newUser = new UserAccountEntity();
            newUser.setUserFullName("System User");
            newUser.setEmailAddress("system.user@example.com");
            newUser.setActive(true);
            newUser.setAccountGuid(UUID.randomUUID().toString());
            newUser.setIsSystemUser(true);
            return userAccountRepository.saveAndFlush(newUser);
        }
    }

    public UserAccountEntity createSystemUserAccount(String username) {
        var newUser = new UserAccountEntity();
        String guid = UUID.randomUUID().toString();
        newUser.setEmailAddress(guid + "@example.com");
        newUser.setAccountGuid(guid);
        newUser.setActive(true);
        newUser.setIsSystemUser(true);
        newUser.setUserFullName(username);
        newUser.setCreatedBy(newUser);
        newUser.setLastModifiedBy(newUser);
        newUser.setCreatedDateTime(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        newUser.setLastModifiedDateTime(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        return userAccountRepository.saveAndFlush(newUser);
    }

    public UserAccountEntity getIntegrationTestUserAccountEntity() {
        List<UserAccountEntity> userAccounts = userAccountRepository.findByEmailAddressIgnoreCase(INTEGRATION_TEST_USER_EMAIL);
        if (userAccounts.isEmpty()) {
            return createIntegrationUser(UUID.randomUUID().toString());
        }
        return userAccounts.get(0);
    }

    /**
     * If we want to create a different user for judge, than for admin, we pass in a unique identifier, otherwise it will change the user.
     *
     * @param identifier unique reference
     * @return the user account
     */
    public UserAccountEntity getIntegrationTestUserAccountEntity(String identifier) {
        String emailAddress = identifier + "@example.com";
        List<UserAccountEntity> userAccounts = userAccountRepository.findByEmailAddressIgnoreCase(emailAddress);
        if (userAccounts.isEmpty()) {
            return createIntegrationUser(UUID.randomUUID().toString(), identifier, emailAddress, true);
        }
        return userAccounts.get(0);
    }

    public UserAccountEntity getSeparateIntegrationTestUserAccountEntity() {
        List<UserAccountEntity> userAccounts = userAccountRepository.findByEmailAddressIgnoreCase(SEPARATE_TEST_USER_EMAIL);
        if (userAccounts.isEmpty()) {
            return createSeparateUser(UUID.randomUUID().toString());
        }
        return userAccounts.get(0);
    }

    public UserAccountEntity getIntegrationTestUserAccountEntityInactive(String identifier) {
        String emailAddress = identifier + "@example.com";
        List<UserAccountEntity> userAccounts = userAccountRepository.findByEmailAddressIgnoreCase(emailAddress);
        if (userAccounts.isEmpty()) {
            return createIntegrationUser(UUID.randomUUID().toString(), identifier, emailAddress, false);
        }
        return userAccounts.get(0);
    }

    public UserAccountEntity createIntegrationUser(String guid) {
        return createIntegrationUser(guid, INTEGRATION_TEST_USER_EMAIL);
    }

    public UserAccountEntity createIntegrationUser(String guid, String emailAddress) {
        return createIntegrationUser(guid, INTEGRATION_TEST_USER_EMAIL, emailAddress, true);
    }

    private UserAccountEntity createIntegrationUser(String guid, String fullName, String emailAddress, boolean active) {
        UserAccountEntity systemUser = userAccountRepository.getReferenceById(SYSTEM_USER_ID);
        var newUser = new UserAccountEntity();
        newUser.setUserFullName(fullName + "FullName");
        newUser.setEmailAddress(emailAddress);
        newUser.setCreatedBy(systemUser);
        newUser.setLastModifiedBy(systemUser);
        newUser.setActive(active);
        newUser.setAccountGuid(guid);
        newUser.setIsSystemUser(false);
        newUser.setCreatedDateTime(CREATED_DATE_TIME);
        newUser.setLastModifiedDateTime(LAST_MODIFIED_DATE_TIME);
        newUser.setLastLoginTime(LAST_LOGIN_TIME);
        return userAccountRepository.saveAndFlush(newUser);
    }


    private UserAccountEntity createSeparateUser(String guid) {
        UserAccountEntity systemUser = userAccountRepository.getReferenceById(SYSTEM_USER_ID);
        var newUser = new UserAccountEntity();
        newUser.setUserFullName("Saad Integration User");
        newUser.setEmailAddress(SEPARATE_TEST_USER_EMAIL);
        newUser.setCreatedBy(systemUser);
        newUser.setLastModifiedBy(systemUser);
        newUser.setActive(true);
        newUser.setAccountGuid(guid);
        newUser.setIsSystemUser(false);
        return dartsPersistence.save(newUser);
    }

    private UserAccountEntity createReusableAuthorisedIntegrationTestUser(boolean reuse,
                                                                          CourthouseEntity... courthouseEntities) {

        var testUser = reuse ? getIntegrationTestUserAccountEntity() :
            createIntegrationUser(UUID.randomUUID().toString(), UUID.randomUUID().toString() + "@test.com");

        for (CourthouseEntity courthouseEntity : courthouseEntities) {

            SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
            if (courthouseEntity != null) {
                addCourthouseToSecurityGroup(securityGroupEntity, courthouseEntity);
            }
            testUser.getSecurityGroupEntities().add(securityGroupEntity);
            testUser = dartsDatabaseSaveStub.save(testUser);
        }

        return testUser;
    }

    private void addCourthouseToSecurityGroup(SecurityGroupEntity securityGroupEntity,
                                              CourthouseEntity courthouseEntity) {
        if (!securityGroupEntity.getCourthouseEntities().contains(courthouseEntity)) {
            securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
            securityGroupRepository.saveAndFlush(securityGroupEntity);
        }
    }
}