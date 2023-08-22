package uk.gov.hmcts.darts.authorisation.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.authorisation.model.Permission;
import uk.gov.hmcts.darts.authorisation.model.Role;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_CLERK;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_MANAGER;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.JUDGE;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@TestInstance(Lifecycle.PER_CLASS)
class AuthorisationServiceTest {

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private AuthorisationService authorisationService;

    @BeforeAll
    void beforeAll() {
        var systemUser = dartsDatabaseStub.createSystemUserAccountEntity();
        var testUser = dartsDatabaseStub.createIntegrationTestUserAccountEntity(systemUser);

        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(36);
        UserAccountEntity judgeUserAccount = new UserAccountEntity();
        judgeUserAccount.setUsername("Test Judge");
        judgeUserAccount.setEmailAddress("test.judge@example.com");
        judgeUserAccount.setSecurityGroupEntities(List.of(judgesSecurityGroup));
        judgeUserAccount.setCreatedBy(testUser);
        judgeUserAccount.setModifiedBy(testUser);
        UserAccountRepository userAccountRepository = dartsDatabaseStub.getUserAccountRepository();
        userAccountRepository.saveAndFlush(judgeUserAccount);

        SecurityGroupEntity bristolStaff = securityGroupRepository.getReferenceById(20);
        SecurityGroupEntity bristolAppr = securityGroupRepository.getReferenceById(35);
        UserAccountEntity bristolUserAccount = new UserAccountEntity();
        bristolUserAccount.setUsername("Test Bristol");
        bristolUserAccount.setEmailAddress("test.bristol@example.com");
        bristolUserAccount.setSecurityGroupEntities(List.of(bristolStaff, bristolAppr));
        bristolUserAccount.setCreatedBy(testUser);
        bristolUserAccount.setModifiedBy(testUser);
        userAccountRepository.saveAndFlush(bristolUserAccount);

        UserAccountEntity newUser = new UserAccountEntity();
        newUser.setUsername("Test New");
        newUser.setEmailAddress("test.new@example.com");
        newUser.setCreatedBy(testUser);
        newUser.setModifiedBy(testUser);
        userAccountRepository.saveAndFlush(newUser);
    }

    @Test
    void shouldGetAuthorisationForTestJudge() {
        UserState judgeUserState = authorisationService.getAuthorisation("test.judge@example.com").orElseThrow();

        assertEquals(1, judgeUserState.getRoles().size());

        Role judgeRole = judgeUserState.getRoles().iterator().next();
        assertEquals(JUDGE.getId(), judgeRole.getRoleId());
        Set<Permission> judgePermissions = judgeRole.getPermissions();
        assertEquals(11, judgePermissions.size());
        assertTrue(judgePermissions.contains(Permission.builder()
                                                 .permissionId(5)
                                                 .permissionName("Read Judges Notes")
                                                 .build()));
        assertTrue(judgePermissions.contains(Permission.builder()
                                                 .permissionId(11)
                                                 .permissionName("Upload Judges Notes")
                                                 .build()));
    }

    @Test
    void shouldGetAuthorisationForTestBristol() {
        UserState userState = authorisationService.getAuthorisation("Test.Bristol@Example.com").orElseThrow();

        assertEquals(2, userState.getRoles().size());

        Iterator<Role> roleIterator = userState.getRoles().iterator();

        Role courtManagerRole = roleIterator.next();
        assertEquals(COURT_MANAGER.getId(), courtManagerRole.getRoleId());
        Set<Permission> courtManagerPermissions = courtManagerRole.getPermissions();
        assertEquals(10, courtManagerPermissions.size());
        assertTrue(courtManagerPermissions.contains(Permission.builder()
                                                        .permissionId(2)
                                                        .permissionName("Approve/Reject Transcription Request")
                                                        .build()));

        Role courtClerkRole = roleIterator.next();
        assertEquals(COURT_CLERK.getId(), courtClerkRole.getRoleId());
        Set<Permission> courtClerkPermissions = courtClerkRole.getPermissions();
        assertEquals(9, courtClerkPermissions.size());
        assertFalse(courtClerkPermissions.contains(Permission.builder()
                                                       .permissionId(2)
                                                       .permissionName("Approve/Reject Transcription Request")
                                                       .build()));
    }

    @Test
    void shouldGetAuthorisationForTestNewUserWithoutAnySecurityGroupRoles() {
        UserState userState = authorisationService.getAuthorisation("test.new@example.com").orElseThrow();

        assertTrue(userState.getUserId() > 0);
        assertEquals("Test New", userState.getUserName());
        assertEquals(0, userState.getRoles().size());
    }

    @Test
    void shouldGetOptionalUserStateForMissingUserAccount() {
        Optional<UserState> userStateOptional = authorisationService.getAuthorisation("test.missing@example.com");

        assertTrue(userStateOptional.isEmpty());
    }

}
