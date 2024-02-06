package uk.gov.hmcts.darts.authorisation.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.model.Permission;
import uk.gov.hmcts.darts.authorisation.model.Role;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;

@TestInstance(Lifecycle.PER_CLASS)
class AuthorisationServiceTest extends IntegrationBase {

    private static final String TEST_JUDGE_EMAIL = "test.judge@example.com";
    private static final String TEST_BRISTOL_EMAIL = "test.bristol@example.com";
    private static final String TEST_NEW_EMAIL = "test.new@example.com";


    @Autowired
    private AuthorisationService authorisationService;

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeAll
    void beforeAll() {
        dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        SecurityGroupRepository securityGroupRepository = dartsDatabase.getSecurityGroupRepository();
        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(-3);
        UserAccountEntity judgeUserAccount = new UserAccountEntity();
        judgeUserAccount.setUserName("Test Judge");
        judgeUserAccount.setUserFullName("Test Judge");
        judgeUserAccount.setEmailAddress(TEST_JUDGE_EMAIL);
        judgeUserAccount.setSecurityGroupEntities(Set.of(judgesSecurityGroup));
        judgeUserAccount.setCreatedBy(testUser);
        judgeUserAccount.setLastModifiedBy(testUser);
        judgeUserAccount.setAccountGuid(UUID.randomUUID().toString());
        judgeUserAccount.setIsSystemUser(false);
        judgeUserAccount.setActive(true);
        UserAccountRepository userAccountRepository = dartsDatabase.getUserAccountRepository();
        userAccountRepository.saveAndFlush(judgeUserAccount);

        SecurityGroupEntity bristolStaff = securityGroupRepository.getReferenceById(-2);
        SecurityGroupEntity bristolAppr = securityGroupRepository.getReferenceById(-1);
        UserAccountEntity bristolUserAccount = new UserAccountEntity();
        bristolUserAccount.setUserName("Test Bristol");
        bristolUserAccount.setUserFullName("Test Bristol");
        bristolUserAccount.setEmailAddress(TEST_BRISTOL_EMAIL);
        bristolUserAccount.setSecurityGroupEntities(Set.of(bristolStaff, bristolAppr));
        bristolUserAccount.setCreatedBy(testUser);
        bristolUserAccount.setLastModifiedBy(testUser);
        bristolUserAccount.setAccountGuid(UUID.randomUUID().toString());
        bristolUserAccount.setIsSystemUser(false);
        bristolUserAccount.setActive(true);
        userAccountRepository.saveAndFlush(bristolUserAccount);

        UserAccountEntity newUser = new UserAccountEntity();
        newUser.setUserName("Test New");
        newUser.setUserFullName("Test New");
        newUser.setEmailAddress(TEST_NEW_EMAIL);
        newUser.setCreatedBy(testUser);
        newUser.setLastModifiedBy(testUser);
        newUser.setAccountGuid(UUID.randomUUID().toString());
        newUser.setActive(true);
        newUser.setIsSystemUser(false);
        userAccountRepository.saveAndFlush(newUser);
    }

    @Test
    void shouldGetAuthorisationForTestJudge() {
        UserState judgeUserState = authorisationService.getAuthorisation(TEST_JUDGE_EMAIL).orElseThrow();

        assertEquals(1, judgeUserState.getRoles().size());

        Role judgeRole = judgeUserState.getRoles().iterator().next();
        assertEquals(JUDGE.getId(), judgeRole.getRoleId());
        Set<Permission> judgePermissions = judgeRole.getPermissions();
        assertEquals(12, judgePermissions.size());
        assertTrue(judgePermissions.contains(Permission.builder()
              .permissionId(5)
              .permissionName("READ_JUDGES_NOTES")
              .build()));
        assertTrue(judgePermissions.contains(Permission.builder()
              .permissionId(11)
              .permissionName("UPLOAD_JUDGES_NOTES")
              .build()));
    }

    @Test
    void shouldGetAuthorisationForTestBristol() {
        UserState userState = authorisationService.getAuthorisation("Test.Bristol@Example.com").orElseThrow();

        assertEquals(2, userState.getRoles().size());

        Iterator<Role> roleIterator = userState.getRoles().iterator();

        Role approverRole = roleIterator.next();
        assertEquals(APPROVER.getId(), approverRole.getRoleId());
        Set<Permission> approverPermissions = approverRole.getPermissions();
        assertEquals(11, approverPermissions.size());
        assertTrue(approverPermissions.contains(Permission.builder()
              .permissionId(2)
              .permissionName("APPROVE_REJECT_TRANSCRIPTION_REQUEST")
              .build()));

        Role requesterRole = roleIterator.next();
        assertEquals(REQUESTER.getId(), requesterRole.getRoleId());
        Set<Permission> requesterPermissions = requesterRole.getPermissions();
        assertEquals(10, requesterPermissions.size());
        assertFalse(requesterPermissions.contains(Permission.builder()
              .permissionId(2)
              .permissionName("APPROVE_REJECT_TRANSCRIPTION_REQUEST")
              .build()));
    }

    @Test
    void shouldGetAuthorisationForTestNewUserWithoutAnySecurityGroupRoles() {
        UserState userState = authorisationService.getAuthorisation(TEST_NEW_EMAIL).orElseThrow();

        assertTrue(userState.getUserId() > 0);
        assertEquals("Test New", userState.getUserName());
        assertEquals(0, userState.getRoles().size());
    }

    @Test
    void shouldGetOptionalUserStateForMissingUserAccount() {
        Optional<UserState> userStateOptional = authorisationService.getAuthorisation("test.missing@example.com");

        assertTrue(userStateOptional.isEmpty());
    }

    @Test
    @Transactional
    void shouldCheckAuthorisationOK() {
        String emailAddress = TEST_BRISTOL_EMAIL;

        var a1Court = dartsDatabase.createCourthouseUnlessExists("A1 COURT");
        var b2Court = dartsDatabase.createCourthouseUnlessExists("B2 COURT");
        var c3Court = dartsDatabase.createCourthouseUnlessExists("C3 COURT");

        var bristolUser = dartsDatabase.getUserAccountRepository().findByEmailAddressIgnoreCase(emailAddress)
              .stream().findFirst()
              .orElseThrow();
        final Iterator<SecurityGroupEntity> bristolUserGroupIt = bristolUser.getSecurityGroupEntities().iterator();
        bristolUserGroupIt.next().getCourthouseEntities().addAll(Set.of(a1Court, b2Court));
        bristolUserGroupIt.next().getCourthouseEntities().addAll(Set.of(b2Court, c3Court));
        dartsDatabase.getUserAccountRepository().saveAndFlush(bristolUser);

        when(mockUserIdentity.getUserAccount()).thenReturn(bristolUser);
        assertDoesNotThrow(() -> authorisationService.checkCourthouseAuthorisation(
              List.of(a1Court, c3Court),
              Set.of(APPROVER, REQUESTER)
        ));
    }

    @Test
    void shouldCheckAuthorisationThrowsDartsApiException() {
        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        var a1Court = dartsDatabase.createCourthouseUnlessExists("A1 COURT");
        var b2Court = dartsDatabase.createCourthouseUnlessExists("B2 COURT");

        var exception = assertThrows(
              DartsApiException.class,
              () -> authorisationService.checkCourthouseAuthorisation(
                    List.of(a1Court, b2Court),
                    Collections.emptySet()
              )
        );
        assertEquals("Could not obtain user details", exception.getMessage());
    }

}
