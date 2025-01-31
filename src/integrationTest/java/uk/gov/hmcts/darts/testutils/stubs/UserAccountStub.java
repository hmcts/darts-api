package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.mockito.ArgumentMatchers.argThat;
import static uk.gov.hmcts.darts.PredefinedPrimaryKeys.TEST_JUDGE_GLOBAL_SECURITY_GROUP_ID;

@Component
@RequiredArgsConstructor
@Deprecated
public class UserAccountStub {

    private static final int SYSTEM_USER_ID = 0;
    public static final String INTEGRATION_TEST_USER_EMAIL = "integrationtest.user@example.com";
    public static final String SEPARATE_TEST_USER_EMAIL = "separateintegrationtest.user@example.com";
    private static final OffsetDateTime LAST_LOGIN_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime CREATED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final CourthouseStub courthouseStub;
    private final CourthouseRepository courthouseRepository;
    private final SecurityGroupStub securityGroupStub;
    private final UserAccountStubComposable userAccountStubComposable;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    public UserAccountEntity getSystemUserAccountEntity() {
        return userAccountStubComposable.getSystemUserAccountEntity();
    }

    public UserAccountEntity createSystemUserAccount(String username) {
        return userAccountStubComposable.createSystemUserAccount(username);
    }

    public UserAccountEntity getIntegrationTestUserAccountEntity() {
        return userAccountStubComposable.getIntegrationTestUserAccountEntity();
    }

    /**
     * If we want to create a different user for judge, than for admin, we pass in a unique identifier, otherwise it will change the user.
     *
     * @param identifier unique reference
     * @return the user account
     */
    public UserAccountEntity getIntegrationTestUserAccountEntity(String identifier) {
        return userAccountStubComposable.getIntegrationTestUserAccountEntity(identifier);
    }

    public UserAccountEntity getSeparateIntegrationTestUserAccountEntity() {
        return userAccountStubComposable.getSeparateIntegrationTestUserAccountEntity();
    }

    public UserAccountEntity getIntegrationTestUserAccountEntityInactive(String identifier) {
        return userAccountStubComposable.getIntegrationTestUserAccountEntityInactive(identifier);
    }

    public UserAccountEntity createIntegrationUser(String guid) {
        return createIntegrationUser(guid, INTEGRATION_TEST_USER_EMAIL);
    }

    public UserAccountEntity createIntegrationUser(String guid, String emailAddress) {
        return createIntegrationUser(guid, INTEGRATION_TEST_USER_EMAIL, emailAddress, true);
    }

    public UserAccountEntity createIntegrationUser(String guid, String fullName, String emailAddress, boolean active) {
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
        return dartsDatabaseSaveStub.save(newUser);
    }


    @Transactional
    public void setActiveState(String email, boolean active) {
        UserAccountEntity userAccountEntity = userAccountRepository.findFirstByEmailAddressIgnoreCase(email).get();
        userAccountEntity.setActive(active);
        userAccountRepository.save(userAccountEntity);
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(String courthouse) {
        return createAuthorisedIntegrationTestUser(courthouseStub.createCourthouseUnlessExists(courthouse));
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(boolean reuse, String courthouse) {
        return createAuthorisedIntegrationTestUser(reuse, courthouseStub.createCourthouseUnlessExists(courthouse));
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(String... courthouses) {
        CourthouseEntity[] courthouseEntities = new CourthouseEntity[courthouses.length];
        for (int i = 0; i < courthouses.length; i++) {
            courthouseEntities[i] = courthouseStub.createCourthouseUnlessExists(courthouses[i]);
        }

        return createAuthorisedIntegrationTestUser(true, courthouseEntities);
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(CourthouseEntity... courthouseEntities) {
        return createReusableAuthorisedIntegrationTestUser(true, courthouseEntities);
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(boolean reuse, CourthouseEntity... courthouseEntities) {
        return createReusableAuthorisedIntegrationTestUser(reuse, courthouseEntities);
    }

    private UserAccountEntity createReusableAuthorisedIntegrationTestUser(boolean reuse,
                                                                          CourthouseEntity... courthouseEntities) {

        var testUser = reuse ? getIntegrationTestUserAccountEntity() :
            createIntegrationUser(UUID.randomUUID().toString(), UUID.randomUUID().toString() + "@test.com");

        for (CourthouseEntity courthouseEntity : courthouseEntities) {
            SecurityGroupEntity securityGroupEntity = securityGroupRepository.findByGroupNameIgnoreCase("Test Transcriber").orElseThrow();

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
            dartsDatabaseSaveStub.save(securityGroupEntity);
        }
    }

    @Transactional
    public UserAccountEntity createUnauthorisedIntegrationTestUser() {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
        securityGroupEntity.getCourthouseEntities().clear();
        dartsDatabaseSaveStub.save(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser = dartsDatabaseSaveStub.save(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createTranscriptionCompanyUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(-4).get();
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = dartsDatabaseSaveStub.save(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createJudgeUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-3);
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = dartsDatabaseSaveStub.save(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createJudgeUser() {
        return createJudgeUser("default");
    }

    @Transactional
    public UserAccountEntity createJudgeUser(String identifier) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(TEST_JUDGE_GLOBAL_SECURITY_GROUP_ID).get();
        securityGroupEntity.getCourthouseEntities().addAll(courthouseRepository.findAll());
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity("Judge" + identifier);
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = dartsDatabaseSaveStub.save(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createUser(String identifier) {
        var testUser = getIntegrationTestUserAccountEntity(identifier);
        testUser = dartsDatabaseSaveStub.save(testUser);
        return testUser;
    }

    @Transactional
    public UserAccountEntity createRcjAppealUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = SecurityGroupTestData
            .buildGroupForRoleAndCourthouse(SecurityRoleEnum.RCJ_APPEALS, courthouseEntity);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = dartsDatabaseSaveStub.save(testUser);
        return testUser;
    }

    public UserAccountEntity createXhibitExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(-14).get();
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }

    public UserAccountEntity createCppExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-15);
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }


    public UserAccountEntity createDarPcExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-16);
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }

    public UserAccountEntity createMidTierExternalUser(String guid, CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(-17).get();
        securityGroupEntity.setGlobalAccess(true);
        securityGroupEntity = dartsDatabaseSaveStub.save(securityGroupEntity);

        return createExternalUser(guid, securityGroupEntity, courthouseEntity);
    }

    public UserAccountEntity createExternalUser(String guid, SecurityGroupEntity securityGroupEntity,
                                                CourthouseEntity courthouseEntity) {

        var testUser = getIntegrationTestUserAccountEntity();
        Set<SecurityGroupEntity> securityGroupEntities = new LinkedHashSet<>();
        securityGroupEntities.add(securityGroupEntity);
        testUser.setSecurityGroupEntities(securityGroupEntities);
        testUser.setIsSystemUser(true);
        testUser.setAccountGuid(guid);

        testUser = dartsDatabaseSaveStub.save(testUser);
        if (nonNull(courthouseEntity)) {
            securityGroupStub.addCourthouse(securityGroupEntity, courthouseEntity);
        }
        return testUser;
    }

    @Transactional
    public UserAccountEntity createSuperAdminUser() {
        var adminGroup = securityGroupRepository.findByGroupNameIgnoreCase("SUPER_ADMIN")
            .orElseThrow();
        adminGroup.setGlobalAccess(true);
        adminGroup = dartsDatabaseSaveStub.save(adminGroup);

        var user = getIntegrationTestUserAccountEntity("adminUserAccount");
        user.getSecurityGroupEntities().clear();
        user.getSecurityGroupEntities().add(adminGroup);

        return dartsDatabaseSaveStub.save(user);
    }

    @Transactional
    public UserAccountEntity createSuperAdminUserInactive() {
        var adminGroup = securityGroupRepository.findByGroupNameIgnoreCase("SUPER_ADMIN")
            .orElseThrow();
        adminGroup.setGlobalAccess(true);
        adminGroup = dartsDatabaseSaveStub.save(adminGroup);

        var user = getIntegrationTestUserAccountEntityInactive("adminUserAccount");
        user.getSecurityGroupEntities().clear();
        user.getSecurityGroupEntities().add(adminGroup);

        return dartsDatabaseSaveStub.save(user);
    }

    @Transactional
    public UserAccountEntity createSuperUser() {
        var superUserGroup = securityGroupRepository.findByGroupNameIgnoreCase("SUPER_USER")
            .orElseThrow();
        superUserGroup.setGlobalAccess(true);
        superUserGroup = dartsDatabaseSaveStub.save(superUserGroup);

        var user = getIntegrationTestUserAccountEntity("superUserAccount");
        user.getSecurityGroupEntities().clear();
        user.getSecurityGroupEntities().add(superUserGroup);

        return dartsDatabaseSaveStub.save(user);
    }

    @Transactional
    public List<UserAccountEntity> createAuthorisedIntegrationTestUsersSystemAndNonSystem(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
        addCourthouseToSecurityGroup(securityGroupEntity, courthouseEntity);

        var testUserNonSystem = getIntegrationTestUserAccountEntity();
        testUserNonSystem.getSecurityGroupEntities().add(securityGroupEntity);
        testUserNonSystem = dartsDatabaseSaveStub.save(testUserNonSystem);

        var testUserSystem = getSeparateIntegrationTestUserAccountEntity();
        testUserSystem.getSecurityGroupEntities().add(securityGroupEntity);
        testUserSystem.setIsSystemUser(true);
        testUserSystem = dartsDatabaseSaveStub.save(testUserSystem);

        return Arrays.asList(testUserNonSystem, testUserSystem);
    }

    public UserAccountEntity givenUserIsAuthorisedJudge(UserIdentity userIdentity) {
        var user = createJudgeUser();

        Mockito.when(userIdentity.getUserAccount())
            .thenReturn(user);
        Mockito.when(userIdentity.userHasGlobalAccess(argThat(t -> t.contains(SecurityRoleEnum.JUDICIARY))))
            .thenReturn(true);

        return user;
    }
}