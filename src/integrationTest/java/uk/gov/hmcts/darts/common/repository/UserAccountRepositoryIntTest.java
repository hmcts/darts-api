package uk.gov.hmcts.darts.common.repository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
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
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

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
        userAccountEntity1 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some.user.email@example.net")
            .build()
            .getEntity();
        dartsPersistence.save(userAccountEntity1);

        userAccountEntity2 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some.user.email-2@example.net")
            .build()
            .getEntity();
        dartsPersistence.save(userAccountEntity2);

        userAccountEntity3 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some.user.email-3@example.net")
            .isSystemUser(true)
            .build()
            .getEntity();
        dartsPersistence.save(userAccountEntity3);
    }

    @Test
    void findByEmailAddressIgnoreCase_shouldReturnUserAccount_whenEmailExistsWithDifferentCase() {
        UserAccountEntity integrationTestUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        Mockito.when(authorisationApi.getCurrentUser()).thenReturn(integrationTestUser);
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        String userEmail = "inteGrationTest.user@EXample.com";

        List<UserAccountEntity> foundUsers = userAccountRepository.findByEmailAddressIgnoreCase(userEmail);

        assertEquals(1, foundUsers.size());
        assertEquals(integrationTestUser.getEmailAddress(), foundUsers.get(0).getEmailAddress());
    }

    @Test
    void findUsers_shouldReturnUser_whenOnlyEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnUser_whenOnlyIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnMultipleUsers_whenOnlyIdsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), null
        );

        assertThat(users, Matchers.hasSize(2));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
        assertThat(users.get(1).getId(), equalTo(userAccountEntity2.getId()));
    }

    @Test
    void findUsers_shouldReturnUsersSortedByIdDescending_whenSortSetToIdDesc() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(2));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity2.getId()));
        assertThat(users.get(1).getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldNotReturnUsers_whenOnlyNonMatchingEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email@example.net", null, null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldNotReturnUsers_whenOnlyNonMatchingIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(123), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldReturnUsers_whenOptionalFieldsBlank() {
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
    void findUsers_shouldReturnUser_whenAllOptionalFieldsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), List.of(userAccountEntity1.getId()), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldNotReturnUser_whenAllOptionalFieldsIncorrect() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email@example.net", List.of(123), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldReturnSystemUser_whenOnlyIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, null, List.of(userAccountEntity3.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity3.getId()));
    }

    @Test
    void findUsers_shouldReturnSystemUser_whenOnlyEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, userAccountEntity3.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.getFirst().getId(), equalTo(userAccountEntity3.getId()));
    }

    @Test
    void findUsers_shouldNotReturnUsers_whenEmailMatchesAndIdDoesNotMatch() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), List.of(123), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldNotReturnUsers_whenIdMatchesAndEmailDoesNotMatch() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email@example.net", List.of(userAccountEntity1.getId()), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldNotReturnUsers_whenSystemUsersExcluded() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity3.getEmailAddress(), List.of(userAccountEntity3.getId()), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldNotReturnUsers_whenSystemUsersIncludedAndIdDoesNotMatch() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, userAccountEntity3.getEmailAddress(), List.of(123), null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findInactiveUsersForCleanupExcludingRoles_shouldReturnEligibleUsers_whenInactiveUsersIncludeExcludedAccounts() {
        OffsetDateTime cutoffDateTime = OffsetDateTime.of(2026, 2, 14, 10, 5, 0, 0, ZoneOffset.UTC);
        final UserAccountEntity oldLastLoginUser = persistUser(
            "old.last.login@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);
        final UserAccountEntity oldNeverLoggedInUser = persistUser(
            "old.never.logged.in@example.net", cutoffDateTime.minusDays(1), null, true, false);
        final UserAccountEntity disabledUserAssignedToGroup = persistUser(
            "disabled.with.group@example.net", cutoffDateTime, cutoffDateTime, false, false);
        dartsDatabase.addUserToGroup(disabledUserAssignedToGroup, SecurityGroupEnum.MEDIA_IN_PERPETUITY);
        final UserAccountEntity disabledUserAssignedTranscription = persistUser(
            "disabled.with.transcription@example.net", cutoffDateTime, cutoffDateTime, false, false);
        createWithTranscriberWorkflow(disabledUserAssignedTranscription, cutoffDateTime);

        persistUser("recent.last.login@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.plusDays(1), true, false);
        persistUser("inactive.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), false, false);
        persistUser("old.localhost.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);

        UserAccountEntity systemUserWithSystemRole = persistUser(
            "system.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, true);

        UserAccountEntity superUser = persistUser(
            "super.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);
        UserAccountEntity superAdmin = persistUser(
            "super.admin@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, false);
        final UserAccountEntity recentDisabledUserAssignedToGroup = persistUser(
            "recent.disabled.with.group@example.net", cutoffDateTime, cutoffDateTime.plusDays(1), false, false);
        final UserAccountEntity localhostUserAssignedToGroup = persistUser(
            "disabled.localhost.user@example.net", cutoffDateTime, cutoffDateTime, false, false);
        final UserAccountEntity systemUserAssignedToGroup = persistUser(
            "disabled.system.user@example.net", cutoffDateTime, cutoffDateTime, false, true);
        final UserAccountEntity superUserAssignedToGroup = persistUser(
            "disabled.super.user@example.net", cutoffDateTime, cutoffDateTime, false, false);
        final UserAccountEntity superAdminAssignedToGroup = persistUser(
            "disabled.super.admin@example.net", cutoffDateTime, cutoffDateTime, false, false);
        dartsDatabase.addUserToGroup(
            systemUserWithSystemRole,
            securityGroupRepository.findByGroupNameIgnoreCase("XHIBIT").orElseThrow()
        );
        dartsDatabase.addUserToGroup(superUser, SecurityGroupEnum.SUPER_USER);
        dartsDatabase.addUserToGroup(superAdmin, SecurityGroupEnum.SUPER_ADMIN);
        dartsDatabase.addUserToGroup(recentDisabledUserAssignedToGroup, SecurityGroupEnum.MEDIA_IN_PERPETUITY);
        dartsDatabase.addUserToGroup(localhostUserAssignedToGroup, SecurityGroupEnum.MEDIA_IN_PERPETUITY);
        dartsDatabase.addUserToGroup(systemUserAssignedToGroup, SecurityGroupEnum.MEDIA_IN_PERPETUITY);
        dartsDatabase.addUserToGroup(superUserAssignedToGroup, SecurityGroupEnum.SUPER_USER);
        dartsDatabase.addUserToGroup(superAdminAssignedToGroup, SecurityGroupEnum.SUPER_ADMIN);

        List<UserAccountEntity> users = userAccountRepository.findInactiveUsersForCleanupExcludingRoles(
            cutoffDateTime,
            Set.of(SUPER_USER.getId(), SUPER_ADMIN.getId()),
            WITH_TRANSCRIBER.getId(),
            Limit.of(10)
        );

        List<Integer> actualIds = users.stream()
            .map(UserAccountEntity::getId)
            .toList();

        assertThat(actualIds, containsInAnyOrder(
            oldLastLoginUser.getId(),
            oldNeverLoggedInUser.getId(),
            disabledUserAssignedToGroup.getId(),
            disabledUserAssignedTranscription.getId()
        ));
    }

    private void createWithTranscriberWorkflow(UserAccountEntity workflowActor, OffsetDateTime workflowTimestamp) {
        TranscriptionEntity transcription = PersistableFactory.getTranscriptionTestData()
            .minimalRawTranscription(new TranscriptionStatusEntity(WITH_TRANSCRIBER.getId()));
        transcription = dartsPersistence.save(transcription);

        TranscriptionWorkflowEntity approvedWorkflow = PersistableFactory.getTranscriptionWorkflowTestData()
            .workflowForTranscriptionWithStatus(transcription, APPROVED);
        approvedWorkflow.setWorkflowActor(workflowActor);
        approvedWorkflow.setWorkflowTimestamp(workflowTimestamp.minusMinutes(1));
        dartsPersistence.save(approvedWorkflow);

        TranscriptionWorkflowEntity withTranscriberWorkflow = PersistableFactory.getTranscriptionWorkflowTestData()
            .workflowForTranscriptionWithStatus(transcription, WITH_TRANSCRIBER);
        withTranscriberWorkflow.setWorkflowActor(workflowActor);
        withTranscriberWorkflow.setWorkflowTimestamp(workflowTimestamp);
        dartsPersistence.save(withTranscriberWorkflow);
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
