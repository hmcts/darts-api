package uk.gov.hmcts.darts.authorisation.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
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
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.COURT_CLERK;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.COURT_MANAGER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@TestInstance(Lifecycle.PER_CLASS)
class AuthorisationServiceTest {

    private static final String TEST_JUDGE_EMAIL = "test.judge@example.com";
    private static final String TEST_BRISTOL_EMAIL = "test.bristol@example.com";
    private static final String TEST_NEW_EMAIL = "test.new@example.com";

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private AuthorisationService authorisationService;

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeAll
    void beforeAll() {
        dartsDatabaseStub.getUserAccountStub().getSystemUserAccountEntity();
        var testUser = dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();

        SecurityGroupRepository securityGroupRepository = dartsDatabaseStub.getSecurityGroupRepository();
        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(36);
        UserAccountEntity judgeUserAccount = new UserAccountEntity();
        judgeUserAccount.setUsername("Test Judge");
        judgeUserAccount.setEmailAddress(TEST_JUDGE_EMAIL);
        judgeUserAccount.setSecurityGroupEntities(Set.of(judgesSecurityGroup));
        judgeUserAccount.setCreatedBy(testUser);
        judgeUserAccount.setLastModifiedBy(testUser);
        UserAccountRepository userAccountRepository = dartsDatabaseStub.getUserAccountRepository();
        userAccountRepository.saveAndFlush(judgeUserAccount);

        SecurityGroupEntity bristolStaff = securityGroupRepository.getReferenceById(20);
        SecurityGroupEntity bristolAppr = securityGroupRepository.getReferenceById(35);
        UserAccountEntity bristolUserAccount = new UserAccountEntity();
        bristolUserAccount.setUsername("Test Bristol");
        bristolUserAccount.setEmailAddress(TEST_BRISTOL_EMAIL);
        bristolUserAccount.setSecurityGroupEntities(Set.of(bristolStaff, bristolAppr));
        bristolUserAccount.setCreatedBy(testUser);
        bristolUserAccount.setLastModifiedBy(testUser);
        userAccountRepository.saveAndFlush(bristolUserAccount);

        UserAccountEntity newUser = new UserAccountEntity();
        newUser.setUsername("Test New");
        newUser.setEmailAddress(TEST_NEW_EMAIL);
        newUser.setCreatedBy(testUser);
        newUser.setLastModifiedBy(testUser);
        userAccountRepository.saveAndFlush(newUser);
    }

    @Test
    void shouldGetAuthorisationForTestJudge() {
        UserState judgeUserState = authorisationService.getAuthorisation(TEST_JUDGE_EMAIL).orElseThrow();

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
        when(mockUserIdentity.getEmailAddress()).thenReturn(emailAddress);

        var a1Court = dartsDatabaseStub.createCourthouseUnlessExists("A1 COURT");
        var b2Court = dartsDatabaseStub.createCourthouseUnlessExists("B2 COURT");
        var c3Court = dartsDatabaseStub.createCourthouseUnlessExists("C3 COURT");

        var bristolUser = dartsDatabaseStub.getUserAccountRepository().findByEmailAddressIgnoreCase(emailAddress)
            .orElseThrow();
        final Iterator<SecurityGroupEntity> bristolUserGroupIt = bristolUser.getSecurityGroupEntities().iterator();
        bristolUserGroupIt.next().getCourthouseEntities().addAll(Set.of(a1Court, b2Court));
        bristolUserGroupIt.next().getCourthouseEntities().addAll(Set.of(b2Court, c3Court));
        dartsDatabaseStub.getUserAccountRepository().saveAndFlush(bristolUser);

        assertDoesNotThrow(() -> authorisationService.checkAuthorisation(List.of(a1Court, c3Court)));
    }

    @Test
    void shouldCheckAuthorisationThrowsDartsApiException() {
        when(mockUserIdentity.getEmailAddress()).thenReturn(TEST_NEW_EMAIL);

        var a1Court = dartsDatabaseStub.createCourthouseUnlessExists("A1 COURT");
        var b2Court = dartsDatabaseStub.createCourthouseUnlessExists("B2 COURT");

        var exception = assertThrows(
            DartsApiException.class,
            () -> authorisationService.checkAuthorisation(List.of(a1Court, b2Court))
        );
        assertEquals("User is not authorised for the associated courthouse", exception.getMessage());
    }

}
