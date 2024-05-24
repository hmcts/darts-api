package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class UserManagementAuditTest extends IntegrationBase {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private GivenBuilder given;

    @Test
    void auditsWhenUsersAreCreated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        userManagementService.createUser(
            new User()
                .fullName("some-full-name")
                .emailAddress("someone@hmcts.net")
                .description("some-description")
                .active(true));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Create User");
    }

    @Test
    void auditsWhenUsersAreDeactivated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var activeUser = dartsDatabase.save(minimalUserAccount());

        userManagementService.modifyUser(activeUser.getId(), new UserPatch().active(false));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Deactivate User");
    }

    @Test
    void auditsWhenUsersAreActivated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var userAccount = minimalUserAccount();
        userAccount.setActive(false);
        var activeUser = dartsDatabase.save(userAccount);

        userManagementService.modifyUser(activeUser.getId(), new UserPatch().active(true));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Reactivate User");
    }

    @Test
    void auditsWhenUsersBasicDetailsAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var userAccount = dartsDatabase.save(minimalUserAccount());

        userManagementService.modifyUser(userAccount.getId(), new UserPatch().fullName("some-new-name"));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Update User");
    }

}
