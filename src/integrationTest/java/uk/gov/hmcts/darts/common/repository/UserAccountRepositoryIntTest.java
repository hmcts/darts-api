package uk.gov.hmcts.darts.common.repository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

class UserAccountRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private DartsPersistence dartsPersistence;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private AuthorisationApi authorisationApi;

    @MockitoBean
    private UserIdentity userIdentity;

    private UserAccountEntity userAccountEntity1;

    private UserAccountEntity userAccountEntity2;

    private UserAccountEntity userAccountEntity3;

    @BeforeEach
    void setUp() {
        UserAccountEntity user1 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some.user.email@example.net")
            .build()
            .getEntity();
        dartsPersistence.save(user1);
        userAccountEntity1 = user1;

        UserAccountEntity user2 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some.user.email-2@example.net")
            .build()
            .getEntity();
        dartsPersistence.save(user2);
        userAccountEntity2 = user2;

        UserAccountEntity user3 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some.user.email-3@example.net")
            .isSystemUser(true)
            .build()
            .getEntity();
        dartsPersistence.save(user3);
        userAccountEntity3 = user3;
    }

    @Test
    void findByEmailAddressIgnoreCase_shouldReturnExpectedUserAccount_whenItExistsIgnoringCase() {
        UserAccountEntity integrationTestUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        Mockito.when(authorisationApi.getCurrentUser()).thenReturn(integrationTestUser);
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        String userEmail = "inteGrationTest.user@EXample.com";

        List<UserAccountEntity> foundUsers = userAccountRepository.findByEmailAddressIgnoreCase(userEmail);

        assertEquals(1, foundUsers.size());
        assertEquals(integrationTestUser.getEmailAddress(), foundUsers.get(0).getEmailAddress());
    }

    @Test
    void findUsers_shouldReturnUsers_WhenOnlyEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnUser_WhenOnlyIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnMultipleUsers_WhenOnlyIDsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), null
        );

        assertThat(users, Matchers.hasSize(2));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
        assertThat(users.get(1).getId(), equalTo(userAccountEntity2.getId()));
    }

    @Test
    void findUsers_shouldSortUsersByIdDesc_WhenSortSetToIdDesc() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(2));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity2.getId()));
        assertThat(users.get(1).getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldNotReturnUsers_WhenOnlyNonMatchingEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email@example.net", null, null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldNotReturnUsers_WhenOnlyNonMatchingIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(123), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldReturnUsers_WhenOptionalFieldsBlank() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, null, null
        );

        List<Integer> actualIds = users.stream()
            .map(UserAccountEntity::getId)
            .toList();

        assertThat(actualIds, containsInAnyOrder(
            userAccountEntity1.getId(),
            userAccountEntity2.getId(),
            -99
        ));
    }

    @Test
    void findUsers_shouldReturnUser_WhenAllOptionalFieldsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), List.of(userAccountEntity1.getId()), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldNotReturnUser_WhenAllOptionalFieldsIncorrect() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email@example.net", List.of(123), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldReturnSystemUser_WhenOnlyIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, null, List.of(userAccountEntity3.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity3.getId()));
    }

    @Test
    void findUsers_shouldReturnSystemUser_WhenOnlyEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, userAccountEntity3.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity3.getId()));
    }

    @Test
    void findUsers_shouldFailWithNonMatchingId_WhenEmailMatches() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), List.of(123), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldFailWithNonMatchingEmail_WhenIdMatches() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email@example.net", List.of(userAccountEntity1.getId()), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldFailWithSystemUser_WhenSystemUsersExcluded() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity3.getEmailAddress(), List.of(userAccountEntity3.getId()), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldFailWithSystemUserIncluded_WhenIdDoesNotMatch() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, userAccountEntity3.getEmailAddress(), List.of(123), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findInactiveUsersExcludingRoles_shouldReturnOnlyEligibleInactiveUsers() {
        OffsetDateTime cutoffDateTime = OffsetDateTime.of(2026, 2, 14, 10, 5, 0, 0, ZoneOffset.UTC);
        UserAccountEntity oldLastLoginUser = persistUser(
            "old.last.login@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);
        UserAccountEntity oldNeverLoggedInUser = persistUser(
            "old.never.logged.in@example.net", cutoffDateTime.minusDays(1), null, true, false);
        persistUser("recent.last.login@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.plusDays(1), true, false);
        persistUser("inactive.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), false, false);

        UserAccountEntity systemUserWithSystemRole = persistUser(
            "system.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, true);

        UserAccountEntity superUser = persistUser(
            "super.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);
        UserAccountEntity superAdmin = persistUser(
            "super.admin@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);
        dartsDatabase.addUserToGroup(
            systemUserWithSystemRole,
            securityGroupRepository.findByGroupNameIgnoreCase("XHIBIT").orElseThrow()
        );
        dartsDatabase.addUserToGroup(superUser, SecurityGroupEnum.SUPER_USER);
        dartsDatabase.addUserToGroup(superAdmin, SecurityGroupEnum.SUPER_ADMIN);

        List<UserAccountEntity> users = userAccountRepository.findInactiveUsersExcludingRoles(
            cutoffDateTime,
            Set.of(SUPER_USER.getId(), SUPER_ADMIN.getId()),
            PageRequest.of(0, 10)
        );

        List<Integer> actualIds = users.stream()
            .map(UserAccountEntity::getId)
            .toList();

        assertThat(actualIds, containsInAnyOrder(oldLastLoginUser.getId(), oldNeverLoggedInUser.getId()));
    }

    private UserAccountEntity persistUser(String emailAddress,
                                          OffsetDateTime createdDateTime,
                                          OffsetDateTime lastLoginTime,
                                          boolean active,
                                          boolean isSystemUser) {
        UserAccountEntity userAccount = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress(emailAddress)
            .createdDateTime(createdDateTime)
            .lastLoginTime(lastLoginTime)
            .active(active)
            .isSystemUser(isSystemUser)
            .securityGroupEntities(new LinkedHashSet<>())
            .build()
            .getEntity();
        UserAccountEntity savedUserAccount = dartsPersistence.save(userAccount);
        savedUserAccount.setCreatedDateTime(createdDateTime);
        savedUserAccount.setLastLoginTime(lastLoginTime);
        return userAccountRepository.saveAndFlush(savedUserAccount);
    }
}
