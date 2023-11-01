package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
@RequiredArgsConstructor
public class UserAccountStub {

    private static final int SYSTEM_USER_ID = 0;
    private static final String INTEGRATION_TEST_USER_EMAIL = "integrationtest.user@example.com";

    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupRepository securityGroupRepository;

    public UserAccountEntity getSystemUserAccountEntity() {

        Optional<UserAccountEntity> userAccountEntityOptional = userAccountRepository.findById(SYSTEM_USER_ID);

        if (userAccountEntityOptional.isPresent()) {
            return userAccountEntityOptional.get();
        } else {
            var newUser = new UserAccountEntity();
            newUser.setUsername("System User");
            newUser.setEmailAddress("system.user@example.com");
            return userAccountRepository.saveAndFlush(newUser);
        }
    }

    public UserAccountEntity getIntegrationTestUserAccountEntity() {
        Optional<UserAccountEntity> userAccountEntityOptional = userAccountRepository.findByEmailAddressIgnoreCase(
            INTEGRATION_TEST_USER_EMAIL);

        if (userAccountEntityOptional.isPresent()) {
            return userAccountEntityOptional.get();
        } else {
            UserAccountEntity systemUser = userAccountRepository.getReferenceById(SYSTEM_USER_ID);
            var newUser = new UserAccountEntity();
            newUser.setUsername("IntegrationTest User");
            newUser.setEmailAddress(INTEGRATION_TEST_USER_EMAIL);
            newUser.setCreatedBy(systemUser);
            newUser.setLastModifiedBy(systemUser);
            newUser.setState(1);
            return userAccountRepository.saveAndFlush(newUser);
        }
    }

    @Transactional
    public UserAccountEntity createAuthorisedIntegrationTestUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(1);
        addCourthouseToSecurityGroup(securityGroupEntity, courthouseEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    private void addCourthouseToSecurityGroup(SecurityGroupEntity securityGroupEntity, CourthouseEntity courthouseEntity) {
        if (!securityGroupEntity.getCourthouseEntities().contains(courthouseEntity)) {
            securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
            securityGroupRepository.saveAndFlush(securityGroupEntity);
        }
    }

    public UserAccountEntity createUnauthorisedIntegrationTestUser() {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(1);
        securityGroupEntity.getCourthouseEntities().clear();
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    public UserAccountEntity createTranscriptionCompanyUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(4);
        assertTrue(securityGroupEntity.getCourthouseEntities().isEmpty());
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

    public UserAccountEntity createJudgeUser(CourthouseEntity courthouseEntity) {
        SecurityGroupEntity securityGroupEntity = securityGroupRepository.getReferenceById(36);
        assertTrue(securityGroupEntity.getCourthouseEntities().isEmpty());
        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
        securityGroupEntity = securityGroupRepository.saveAndFlush(securityGroupEntity);

        var testUser = getIntegrationTestUserAccountEntity();
        testUser.getSecurityGroupEntities().clear();
        testUser.getSecurityGroupEntities().add(securityGroupEntity);
        testUser = userAccountRepository.saveAndFlush(testUser);
        return testUser;
    }

}
