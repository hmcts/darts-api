package uk.gov.hmcts.darts.common.repository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserAccountRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DartsPersistence dartsPersistence;

    private UserAccountEntity userAccountEntity1;

    private UserAccountEntity userAccountEntity2;

    private UserAccountEntity userAccountEntity3;

    @BeforeEach
    void setUpData() {
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
    void findUsers_shouldReturnUser_whenOnlyEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnUser_whenOnlyIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnMultipleUsers_whenOnlyIdsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), null
        );
        assertThat(users, Matchers.hasSize(2));
        assertThat(
            users.stream().map(UserAccountEntity::getId).toList(),
            containsInAnyOrder(userAccountEntity1.getId(), userAccountEntity2.getId())
        );
    }

    @Test
    void findUsers_shouldReturnUsersSortedByIdDescending_whenSortSetToIdDesc() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), Sort.by(Sort.Direction.DESC, "id")
        );
        assertThat(users, Matchers.hasSize(2));
        UserAccountEntity firstFoundUser = users.get(0);
        UserAccountEntity secondFoundUser = users.get(1);
        assertThat(firstFoundUser.getId(), equalTo(userAccountEntity2.getId()));
        assertThat(secondFoundUser.getId(), equalTo(userAccountEntity1.getId()));
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
            -99  // migration user added by flyway
        ));
    }

    @Test
    void findUsers_shouldReturnUser_whenAllOptionalFieldsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), List.of(userAccountEntity1.getId()), Sort.by(Sort.Direction.DESC, "id")
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
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
        assertThat(users.get(0).getId(), equalTo(userAccountEntity3.getId()));
    }

    @Test
    void findUsers_shouldReturnSystemUser_whenOnlyEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, userAccountEntity3.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity3.getId()));
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
        persistUser("system.user@example.net", cutoffDateTime.minusDays(1), cutoffDateTime.minusDays(1), true, true);

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

    @ParameterizedTest
    @MethodSource("provideTestCombinations")
    void findUsers_shouldFailWithOneNonMatchingField_WhenOtherFieldsMatch(boolean includeSystemUsers, String emailAddress, List<Integer> userIds) {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            includeSystemUsers, emailAddress, userIds, null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    private Stream<Arguments> provideTestCombinations() {
        return Stream.of(
            Arguments.of(false, userAccountEntity1.getEmailAddress(), List.of(123)), // Non-matching ID
            Arguments.of(false, "non-matching-email@example.net", List.of(userAccountEntity1.getId())), // Non-matching Email
            Arguments.of(false, userAccountEntity3.getEmailAddress(), List.of(userAccountEntity3.getId())), // Include system user false
            Arguments.of(true, userAccountEntity3.getEmailAddress(), List.of(123)) // Include system user true, non-matching ID
        );
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
