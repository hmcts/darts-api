package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class UserQueryTest extends IntegrationBase {

    @Autowired
    private UserManagementQuery userManagementQuery;

    @Test
    void searchWithAllOptionalFieldsBlank() {
        UserAccountEntity user1 = minimalUserAccount();
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(false, null, null);

        assertThat(users).extracting("id")
            .isEqualTo(userIdsDesc(user1, user2, user3));
    }
    @Test
    void searchWithOnlyEmailSpecified() {
        UserAccountEntity user1 = minimalUserAccount();
        user1.setEmailAddress("some-user-email");
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(false, user1.getEmailAddress(), null);

        assertThat(users).extracting("id").containsExactly(user1.getId());
    }

    @Test
    void searchWithOneUserIdSpecified() {
        UserAccountEntity user1 = minimalUserAccount();
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(false, null, List.of(user1.getId()));

        assertThat(users).extracting("id").containsExactly(user1.getId());
    }

    @Test
    void searchWithMultipleUserIdsSpecified() {
        UserAccountEntity user1 = minimalUserAccount();
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(false, null, List.of(user1.getId(), user3.getId()));

        assertThat(users).extracting("id")
            .isEqualTo(userIdsDesc(user1, user3));
    }

    @Test
    void returnsEmptyListIfNoUsersMatchOnMultiplePredicates() {
        UserAccountEntity user1 = minimalUserAccount();
        user1.setEmailAddress("some-user-email");
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(false, user1.getEmailAddress(), List.of(user3.getId()));

        assertThat(users).isEmpty();
    }


    @Test
    void getUsers_shouldIncludeSystemUsers_whenIncludeSystemUsersIsTrue() {
        UserAccountEntity user1 = minimalUserAccount();
        user1.setIsSystemUser(true);
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(true, null, List.of(user1.getId(), user3.getId()));

        assertThat(users).extracting("id")
            .isEqualTo(userIdsDesc(user1, user3));
    }

    @Test
    void getUsers_shouldIncludeSystemUsers_whenIncludeSystemUsersIsFalse() {
        UserAccountEntity user1 = minimalUserAccount();
        user1.setIsSystemUser(true);
        UserAccountEntity user2 = minimalUserAccount();
        UserAccountEntity user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(false, null, List.of(user1.getId(), user3.getId()));

        assertThat(users).extracting("id")
            .isEqualTo(userIdsDesc(user3));
    }

    private static List<Integer> userIdsDesc(UserAccountEntity... users) {
        return Stream.of(users)
            .map(UserAccountEntity::getId)
            .sorted((u1, u2) -> u2 - u1)
            .toList();
    }
}
