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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_CLERK;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_MANAGER;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.JUDGE;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@TestInstance(Lifecycle.PER_CLASS)
class AuthorisationServiceTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private AuthorisationService authorisationService;

    @BeforeAll
    void beforeAll() {
        SecurityGroupEntity judgesSecurityGroup = securityGroupRepository.getReferenceById(36);
        UserAccountEntity judgeUserAccount = new UserAccountEntity();
        judgeUserAccount.setUsername("Test Judge");
        judgeUserAccount.setEmailAddress("test.judge@example.com");
        judgeUserAccount.setSecurityGroupEntities(List.of(judgesSecurityGroup));
        userAccountRepository.saveAndFlush(judgeUserAccount);

        SecurityGroupEntity bristolStaff = securityGroupRepository.getReferenceById(20);
        SecurityGroupEntity bristolAppr = securityGroupRepository.getReferenceById(35);
        UserAccountEntity bristolUserAccount = new UserAccountEntity();
        bristolUserAccount.setUsername("Test Bristol");
        bristolUserAccount.setEmailAddress("test.bristol@example.com");
        bristolUserAccount.setSecurityGroupEntities(List.of(bristolStaff, bristolAppr));
        userAccountRepository.saveAndFlush(bristolUserAccount);
    }

    @Test
    void shouldGetAuthorisationForTestJudge() {
        UserState judgeUserState = authorisationService.getAuthorisation("test.judge@example.com");

        assertNotNull(judgeUserState);
        assertEquals(1, judgeUserState.getRoles().size());

        Role judgeRole = judgeUserState.getRoles().iterator().next();
        assertEquals(JUDGE.getId(), judgeRole.getRoleId());
        Set<Permission> judgePermissions = judgeRole.getPermissions();
        assertEquals(11, judgePermissions.size());
        assertTrue(judgePermissions.contains(new Permission(5, "Read Judges Notes")));
        assertTrue(judgePermissions.contains(new Permission(11, "Upload Judges Notes")));
    }

    @Test
    void shouldGetAuthorisationForTestBristol() {
        UserState userState = authorisationService.getAuthorisation("Test.Bristol@Example.com");

        assertNotNull(userState);
        assertEquals(2, userState.getRoles().size());

        Iterator<Role> roleIterator = userState.getRoles().iterator();

        Role courtManagerRole = roleIterator.next();
        assertEquals(COURT_MANAGER.getId(), courtManagerRole.getRoleId());
        Set<Permission> courtManagerPermissions = courtManagerRole.getPermissions();
        assertEquals(10, courtManagerPermissions.size());
        assertTrue(courtManagerPermissions.contains(new Permission(2, "Approve/Reject Transcription Request")));

        Role courtClerkRole = roleIterator.next();
        assertEquals(COURT_CLERK.getId(), courtClerkRole.getRoleId());
        Set<Permission> courtClerkPermissions = courtClerkRole.getPermissions();
        assertEquals(9, courtClerkPermissions.size());
        assertFalse(courtClerkPermissions.contains(new Permission(2, "Approve/Reject Transcription Request")));
    }

}
