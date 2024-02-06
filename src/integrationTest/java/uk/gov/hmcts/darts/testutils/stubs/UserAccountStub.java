package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class UserAccountStub {

    private static final int SYSTEM_USER_ID = 0;
    private static final String INTEGRATION_TEST_USER_EMAIL = "integrationtest.user@example.com";
    private static final String SEPARATE_TEST_USER_EMAIL = "separateintegrationtest.user@example.com";
    private static final OffsetDateTime LAST_LOGIN_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime CREATED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final CourthouseStub courthouseStub;
    private final CourthouseRepository courthouseRepository;

    public UserAccountEntity getSystemUserAccountEntity() {

        Optional<UserAccountEntity> userAccountEntityOptional = userAccountRepository.findById(SYSTEM_USER_ID);

        if (userAccountEntityOptional.isPresent()) {
            return userAccountEntityOptional.get();
        } else {
            var newUser = new UserAccountEntity();
            newUser.setUserName("System User");
            newUser.setEmailAddress("system.user@example.com");
            newUser.setActive(true);
            newUser.setAccountGuid(UUID.randomUUID().toString());
            newUser.setIsSystemUser(true);
            return userAccountRepository.saveAndFlush(newUser);
        }
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
            return createIntegrationUser(UUID.randomUUID().toString(), identifier, emailAddress);
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


    private UserAccountEntity createIntegrationUser(String guid) {
        return createIntegrationUser(guid, INTEGRATION_TEST_USER_EMAIL);
    }

    private UserAccountEntity createIntegrationUser(String guid, String emailAddress) {
        return createIntegrationUser(guid, INTEGRATION_TEST_USER_EMAIL, emailAddress);
    }

    private UserAccountEntity createIntegrationUser(String guid, String fullName, String emailAddress) {
        UserAccountEntity systemUser = userAccountRepository.getReferenceById(SYSTEM_USER_ID);
        var newUser = new UserAccountEntity();
        newUser.setUserName(fullName + "Username");
        newUser.setUserFullName(fullName + "FullName");
        newUser.setEmailAddress(emailAddress);
        newUser.setCreatedBy(systemUser);
        newUser.setLastModifiedBy(systemUser);
        newUser.setActive(true);
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
        newUser.setUserName("Saad Integration User");
        newUser.setUserFullName("Saad Integration User");
        newUser.setEmailAddress(SEPARATE_TEST_USER_EMAIL);
        newUser.setCreatedBy(systemUser);
        newUser.setLastModifiedBy(systemUser);
        newUser.setActive(true);
        newUser.setAccountGuid(guid);
        newUser.setIsSystemUser(false);
        return userAccountRepository.saveAndFlush(newUser);
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(String courthouse) {
        return createAuthorisedIntegrationTestUser(courthouseStub.createCourthouseUnlessExists(courthouse));
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
        addCourthouseToSecurityGroup(securityGroupEntity, courthouseEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUserWithoutCourthouse() {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    private void addCourthouseToSecurityGroup(SecurityGroupEntity securityGroupEntity,
                                              CourthouseEntity courthouseEntity) {
        if (!securityGroupEntity.getCourthouseEntities().contains(courthouseEntity)) {
            securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
            securityGroupRepository.saveAndFlush(securityGroupEntity);
        }
    }

    @Transactional
    public UserAccountEntity createUnauthorisedIntegrationTestUser() {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
        securityGroupEntity.getCourthouseEntities().clear();
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createTranscriptionCompanyUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(-4).get();
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createJudgeUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-3);
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createJudgeUser() {
        return createJudgeUser("default");
    }

    @Transactional
    public UserAccountEntity createJudgeUser(String identifier) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(-3).get();
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity.getCourthouseEntities().addAll(courthouseRepository.findAll());
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity("Judge" + identifier);
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    public UserAccountEntity createXhibitExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-14);
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }

    public UserAccountEntity createCppExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-15);
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }


    public UserAccountEntity createDarPcExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-16);
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }

    public UserAccountEntity createMidTierExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-17);
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }

    public UserAccountEntity createExternalUser(String guid, SecurityGroupEntity securityGroupEntity,
                                                CourthouseEntity courthouseEntity) {
        if (nonNull(courthouseEntity)) {
            securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        }

        var testUser = getIntegrationTestUserAccountEntity();
        Set<SecurityGroupEntity> securityGroupEntities = new LinkedHashSet<>();
        securityGroupEntities.add(securityGroupEntity);
        testUser.setSecurityGroupEntities(securityGroupEntities);
        testUser.setIsSystemUser(true);
        testUser.setAccountGuid(guid);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createAdminUser() {
        var adminGroup = securityGroupRepository.findByGroupName("ADMIN")
              .orElseThrow();
        adminGroup.setGlobalAccess(true);
        adminGroup.getCourthouseEntities().clear();
        adminGroup.getCourthouseEntities().addAll(courthouseRepository.findAll());
        adminGroup = securityGroupRepository.saveAndFlush(adminGroup);

        var user = getIntegrationTestUserAccountEntity("adminUserAccount");
        user.getSecurityGroupEntities().clear();
        user.getSecurityGroupEntities().add(adminGroup);

        return userAccountRepository.saveAndFlush(user);
    }

}
