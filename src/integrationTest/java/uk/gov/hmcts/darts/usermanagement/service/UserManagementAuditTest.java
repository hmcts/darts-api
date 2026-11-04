package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

class UserManagementAuditTest extends IntegrationBase {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private DisableInactiveUserAccountsService disableInactiveUserAccountsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void createUser_shouldRecordCreateUserAuditAndInsertRevision_whenUserIsCreated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var user = createUser(true);

        transactionalUtil.executeInTransaction(() -> {
            var createUserActivity = findAuditActivity("Create User", dartsDatabase.findAudits());
            assertThat(createUserActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var userAccountRevisions = dartsDatabase.findUserAccountRevisionsFor(user.getId());
            assertThat(userAccountRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
        });
    }

    @Test
    void modifyUser_shouldRecordDeactivateUserAuditAndUpdateRevision_whenActiveUserIsDeactivated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var existingUser = someActiveUserExists();

        userManagementService.modifyUser(existingUser.getId(), new UserPatch().active(false));

        transactionalUtil.executeInTransaction(() -> {
            var deactivateUserActivity = findAuditActivity("Deactivate User", dartsDatabase.findAudits());
            assertThat(deactivateUserActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var userAccountRevisions = dartsDatabase.findUserAccountRevisionsFor(existingUser.getId());
            assertThat(userAccountRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    @Test
    void process_shouldRecordDeactivateUserAuditAndDeactivateAccount_whenUserIsInactiveForConfiguredPeriod() {
        var systemUser = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var inactiveUser = createUser(true);
        var inactiveUserEntity = userAccountRepository.findById(inactiveUser.getId()).orElseThrow();
        inactiveUserEntity.setLastLoginTime(OffsetDateTime.now().minusMonths(7));
        userAccountRepository.save(inactiveUserEntity);

        disableInactiveUserAccountsService.process(1000);

        transactionalUtil.executeInTransaction(() -> {
            var deactivateUserActivity = findAuditActivity("Deactivate User", dartsDatabase.findAudits());
            assertThat(deactivateUserActivity.getUser().getId()).isEqualTo(systemUser.getId());
            assertThat(deactivateUserActivity.getAdditionalData()).isNull();
            assertThat(userAccountRepository.findById(inactiveUser.getId()).orElseThrow().isActive()).isFalse();
        });
    }

    @Test
    void modifyUser_shouldRecordReactivateUserAuditAndUpdateRevision_whenInactiveUserIsActivated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var inactiveUser = someInactiveUserExists();

        userManagementService.modifyUser(inactiveUser.getId(), new UserPatch().active(true));
        transactionalUtil.executeInTransaction(() -> {
            var reactivateUserActivity = findAuditActivity("Reactivate User", dartsDatabase.findAudits());
            assertThat(reactivateUserActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var userAccountRevisions = dartsDatabase.findUserAccountRevisionsFor(inactiveUser.getId());
            assertThat(userAccountRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    @Test
    void modifyUser_shouldRecordUpdateUserAuditAndUpdateRevision_whenBasicDetailsAreChanged() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var existingUser = someUserWithDefaultsExists();

        userManagementService.modifyUser(existingUser.getId(), new UserPatch().fullName("some-new-name"));
        transactionalUtil.executeInTransaction(() -> {
            var updateUserActivity = findAuditActivity("Update User", dartsDatabase.findAudits());
            assertThat(updateUserActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var userAccountRevisions = dartsDatabase.findUserAccountRevisionsFor(existingUser.getId());
            assertThat(userAccountRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    private UserWithId someInactiveUserExists() {
        return createUser(false);
    }

    private UserWithId someActiveUserExists() {
        return createUser(true);
    }

    private UserWithId someUserWithDefaultsExists() {
        return someActiveUserExists();
    }

    private UserWithId createUser(boolean active) {

        return userManagementService.createUser(
            new User()
                .fullName("some-full-name")
                .emailAddress("someone@hmcts.net")
                .description("some-description")
                .active(active));

    }

    private AuditEntity findAuditActivity(String activity, List<AuditEntity> audits) {
        return audits.stream()
            .filter(audit -> activity.equals(audit.getAuditActivity().getName()))
            .findFirst().orElseThrow();
    }
}
