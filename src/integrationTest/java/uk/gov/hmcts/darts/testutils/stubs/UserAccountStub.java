package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
@RequiredArgsConstructor
public class UserAccountStub {

    private static final int SYSTEM_USER_ID = 0;
    private static final String INTEGRATION_TEST_USER_EMAIL = "integrationtest.user@example.com";
    private static final String SEPARATE_TEST_USER_EMAIL = "separateintegrationtest.user@example.com";

    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final CourthouseStub courthouseStub;

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

    public UserAccountEntity getSeparateIntegrationTestUserAccountEntity() {
        List<UserAccountEntity> userAccounts = userAccountRepository.findByEmailAddressIgnoreCase(SEPARATE_TEST_USER_EMAIL);
        if (userAccounts.isEmpty()) {
            return createSeparateUser(UUID.randomUUID().toString());
        }
        return userAccounts.get(0);
    }


    private UserAccountEntity createIntegrationUser(String guid) {
        UserAccountEntity systemUser = userAccountRepository.getReferenceById(SYSTEM_USER_ID);
        var newUser = new UserAccountEntity();
        newUser.setUserName("IntegrationTest User");
        newUser.setEmailAddress(INTEGRATION_TEST_USER_EMAIL);
        newUser.setCreatedBy(systemUser);
        newUser.setLastModifiedBy(systemUser);
        newUser.setActive(true);
        newUser.setAccountGuid(guid);
        newUser.setIsSystemUser(false);
        return userAccountRepository.saveAndFlush(newUser);
    }


    private UserAccountEntity createSeparateUser(String guid) {
        UserAccountEntity systemUser = userAccountRepository.getReferenceById(SYSTEM_USER_ID);
        var newUser = new UserAccountEntity();
        newUser.setUserName("Saad Integration User");
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

    public UserAccountEntity createTranscriptionCompanyUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-4);
        assertTrue(securityGroupEntity.getCourthouseEntities().isEmpty());
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    public UserAccountEntity createJudgeUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(-3);
        assertTrue(securityGroupEntity.getCourthouseEntities().isEmpty());
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
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

    public UserAccountEntity createAdminUser() {
        var adminGroup = securityGroupRepository.findByGroupName("ADMIN")
            .orElseThrow();

        var user = getIntegrationTestUserAccountEntity();
        user.setSecurityGroupEntities(Collections.singleton(adminGroup));

        return userAccountRepository.saveAndFlush(user);
    }

}
