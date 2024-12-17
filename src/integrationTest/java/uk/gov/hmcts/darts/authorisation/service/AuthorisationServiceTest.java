package uk.gov.hmcts.darts.authorisation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EntityGraphPersistence;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.minimalSecurityGroup;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class AuthorisationServiceTest extends IntegrationBase {

    private static final String TEST_JUDGE_EMAIL = "test.judge@example.com";
    private static final String TEST_JUDGE_GLOBAL_EMAIL = "test.judge.global@example.com";
    private static final String TEST_BRISTOL_EMAIL = "test.bristol@example.com";
    private static final String TEST_NEW_EMAIL = "test.new@example.com";
    private static final String TEST_MULTIPLE_USER_EMAIL = "test.multiple.user@example.com";
    private static final int TEST_JUDGE_GROUP_ID = -3;
    private static final int REQUESTOR_SG_ID = -2;
    private static final int APPROVER_SG_ID = -1;


    @Autowired
    private AuthorisationService authorisationService;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private EntityGraphPersistence entityGraphPersistence;

    @BeforeEach
    void beforeEach() {
        dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();

        SecurityGroupRepository securityGroupRepository = dartsDatabase.getSecurityGroupRepository();

        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(TEST_JUDGE_GROUP_ID);

        UserAccountEntity judgeUserAccount = new UserAccountEntity();
        judgeUserAccount.setSecurityGroupEntities(Set.of(judgesSecurityGroup));
        judgeUserAccount.setUserName("Test Judge");
        judgeUserAccount.setUserFullName("Test Judge");
        judgeUserAccount.setEmailAddress(TEST_JUDGE_EMAIL);
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        judgeUserAccount.setCreatedBy(testUser);
        judgeUserAccount.setLastModifiedBy(testUser);
        judgeUserAccount.setAccountGuid(UUID.randomUUID().toString());
        judgeUserAccount.setIsSystemUser(false);
        judgeUserAccount.setActive(true);
        UserAccountRepository userAccountRepository = dartsDatabase.getUserAccountRepository();
        userAccountRepository.saveAndFlush(judgeUserAccount);

        SecurityGroupEntity globalSecurityGroup = SecurityGroupTestData.createGroupForRole(JUDICIARY);
        globalSecurityGroup.setGlobalAccess(true);
        dartsDatabase.getSecurityGroupRepository().saveAndFlush(globalSecurityGroup);

        UserAccountEntity judgeUserAccountGlobal = new UserAccountEntity();
        judgeUserAccountGlobal.setSecurityGroupEntities(Set.of(globalSecurityGroup, judgesSecurityGroup));
        judgeUserAccountGlobal.setUserName("Test Judge Global");
        judgeUserAccountGlobal.setUserFullName("Test Judge Global");
        judgeUserAccountGlobal.setEmailAddress(TEST_JUDGE_GLOBAL_EMAIL);
        judgeUserAccountGlobal.setCreatedBy(testUser);
        judgeUserAccountGlobal.setLastModifiedBy(testUser);
        judgeUserAccountGlobal.setAccountGuid(UUID.randomUUID().toString());
        judgeUserAccountGlobal.setIsSystemUser(false);
        judgeUserAccountGlobal.setActive(true);
        userAccountRepository.saveAndFlush(judgeUserAccountGlobal);

        SecurityGroupEntity bristolStaff = securityGroupRepository.getReferenceById(REQUESTOR_SG_ID);
        SecurityGroupEntity bristolAppr = securityGroupRepository.getReferenceById(APPROVER_SG_ID);
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

        createUser("Test Multiple User 1", TEST_MULTIPLE_USER_EMAIL, true, testUser, userAccountRepository);
        createUser("Test Multiple User 2", TEST_MULTIPLE_USER_EMAIL, false, testUser, userAccountRepository);
    }

    @BeforeEach
    void startHibernateSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    private void addCourthouseToSecurityGroup(CourthouseEntity courthouseEntity, Integer securityGroupId) {

        var securityGroupEntity = dartsDatabase.getSecurityGroupRepository().findById(securityGroupId);

        if (securityGroupEntity.isPresent()) {
            var securityGroup = securityGroupEntity.get();
            securityGroup.setCourthouseEntities(asSet(courthouseEntity));
            dartsDatabase.getSecurityGroupRepository().save(securityGroup);
        }
    }

    private void createUser(String userFullName, String userEmailAddress, Boolean isActive,
                            UserAccountEntity testUser, UserAccountRepository userAccountRepository ) {
        UserAccountEntity newUser = new UserAccountEntity();
        newUser.setUserFullName(userFullName);
        newUser.setEmailAddress(userEmailAddress);
        newUser.setCreatedBy(testUser);
        newUser.setLastModifiedBy(testUser);
        newUser.setAccountGuid(UUID.randomUUID().toString());
        newUser.setActive(isActive);
        newUser.setIsSystemUser(false);
        userAccountRepository.saveAndFlush(newUser);
    }

    @Test
    void shouldGetAuthorisationForTestJudge() {
        SecurityGroupRepository securityGroupRepository = dartsDatabase.getSecurityGroupRepository();

        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(TEST_JUDGE_GROUP_ID);
        var courthouseEntity = dartsDatabase.createCourthouseUnlessExists("FUNC-courthouse-auth-test");
        addCourthouseToSecurityGroup(courthouseEntity, judgesSecurityGroup.getId());

        UserState judgeUserState = authorisationService.getAuthorisation(TEST_JUDGE_EMAIL).orElseThrow();

        assertEquals(1, judgeUserState.getRoles().size());

        UserStateRole judgeRole = judgeUserState.getRoles().iterator().next();
        assertEquals(JUDICIARY.getId(), judgeRole.getRoleId());
        assertFalse(judgeRole.getGlobalAccess());

        assertTrue(judgeRole.getCourthouseIds().contains(courthouseEntity.getId()));

        Set<String> judgePermissions = judgeRole.getPermissions();
        assertEquals(0, judgePermissions.size());
    }

    @Test
    void shouldGetAuthorisationForTestJudgeWithGlobalAccess() {
        SecurityGroupRepository securityGroupRepository = dartsDatabase.getSecurityGroupRepository();

        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(TEST_JUDGE_GROUP_ID);
        var courthouseEntity = dartsDatabase.createCourthouseUnlessExists("FUNC-courthouse-auth-test-global");
        addCourthouseToSecurityGroup(courthouseEntity, judgesSecurityGroup.getId());

        UserState judgeUserState = authorisationService.getAuthorisation(TEST_JUDGE_GLOBAL_EMAIL).orElseThrow();

        assertEquals(1, judgeUserState.getRoles().size());

        UserStateRole judgeRole = judgeUserState.getRoles().iterator().next();
        assertEquals(JUDICIARY.getId(), judgeRole.getRoleId());
        assertTrue(judgeRole.getGlobalAccess());

        assertTrue(judgeRole.getCourthouseIds().contains(courthouseEntity.getId()));

        Set<String> judgePermissions = judgeRole.getPermissions();
        assertEquals(0, judgePermissions.size());
    }

    @Test
    void shouldGetAuthorisationForTestBristol() {
        UserState userState = authorisationService.getAuthorisation("Test.Bristol@Example.com").orElseThrow();

        assertEquals(2, userState.getRoles().size());

        Iterator<UserStateRole> roleIterator = userState.getRoles().iterator();

        UserStateRole approverRole = roleIterator.next();
        assertEquals(REQUESTER.getId(), approverRole.getRoleId());
        assertFalse(approverRole.getGlobalAccess());
        Set<String> approverPermissions = approverRole.getPermissions();
        assertEquals(0, approverPermissions.size());

        UserStateRole requesterRole = roleIterator.next();
        assertEquals(APPROVER.getId(), requesterRole.getRoleId());
        Set<String> requesterPermissions = requesterRole.getPermissions();
        assertEquals(0, requesterPermissions.size());
    }

    @Test
    void shouldGetAuthorisationForTestNewUserWithoutAnySecurityGroupRoles() {
        UserState userState = authorisationService.getAuthorisation(TEST_NEW_EMAIL).orElseThrow();

        assertTrue(userState.getUserId() > 0);
        assertEquals("Test New", userState.getUserName());
        assertEquals(0, userState.getRoles().size());
    }

    @Test
    void shouldGetAuthorisationForTestMultipleUserWithoutAnySecurityGroupRoles() {
        UserState userState = authorisationService.getAuthorisation(TEST_MULTIPLE_USER_EMAIL).orElseThrow();

        assertTrue(userState.getUserId() > 0);
        assertEquals("Test Multiple User 1", userState.getUserName());
        assertEquals(0, userState.getRoles().size());
    }

    @Test
    void shouldGetOptionalUserStateForMissingUserAccount() {
        Optional<UserState> userStateOptional = authorisationService.getAuthorisation("test.missing@example.com");

        assertTrue(userStateOptional.isEmpty());
    }

    @Test
    void shouldCheckAuthorisationOK() {
        var a1Court = createCourthouseWithName("A1 COURT");
        var b2Court = createCourthouseWithName("B2 COURT");
        var c3Court = createCourthouseWithName("C3 COURT");

        var bristolUser = minimalUserAccount();
        bristolUser.setEmailAddress(TEST_BRISTOL_EMAIL);
        when(mockUserIdentity.getUserAccount()).thenReturn(bristolUser);

        var secGrpForApprover = minimalSecurityGroup(bristolUser);
        secGrpForApprover.setSecurityRoleEntity(dartsDatabase.findSecurityRole(APPROVER));
        secGrpForApprover.setCourthouseEntities(asSet(a1Court, b2Court));

        var secGrpForRequestor = minimalSecurityGroup(bristolUser);
        secGrpForRequestor.setSecurityRoleEntity(dartsDatabase.findSecurityRole(REQUESTER));
        secGrpForRequestor.setCourthouseEntities(asSet(b2Court, c3Court));
        entityGraphPersistence.persistAll(List.of(secGrpForApprover, secGrpForRequestor));

        bristolUser.setSecurityGroupEntities(asSet(secGrpForApprover, secGrpForRequestor));

        entityGraphPersistence.persist(bristolUser);

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