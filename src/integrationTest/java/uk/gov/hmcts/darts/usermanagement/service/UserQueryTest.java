package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.testutils.data.UserAccountTestData.minimalUserAccount;

class UserQueryTest extends IntegrationBase {

    @Autowired
    private UserManagementQuery userManagementQuery;
    private UserAccountEntity user1;
    private UserAccountEntity user2;
    private UserAccountEntity user3;

    @AfterEach
    void tearDown() {
        dartsDatabase.addToUserAccountTrash(user1.getEmailAddress(), user2.getEmailAddress(), user3.getEmailAddress());
    }

    @Test
    void searchWithAllOptionalFieldsBlank() {
        user1 = minimalUserAccount();
        user2 = minimalUserAccount();
        user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(null, null);

        assertThat(users).extracting("id")
            .isEqualTo(userIdsDesc(user1, user2, user3));
    }

    @Test
    void searchWithOnlyEmailSpecified() {
        user1 = minimalUserAccount();
        user1.setEmailAddress("some-user-email");
        user2 = minimalUserAccount();
        user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(user1.getEmailAddress(), null);

        assertThat(users).extracting("id").containsExactly(user1.getId());
    }

    @Test
    void searchWithOneUserIdSpecified() {
        user1 = minimalUserAccount();
        user2 = minimalUserAccount();
        user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(null, List.of(user1.getId()));

        assertThat(users).extracting("id").containsExactly(user1.getId());
    }

    @Test
    void searchWithMultipleUserIdsSpecified() {
        user1 = minimalUserAccount();
        user2 = minimalUserAccount();
        user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(null, List.of(user1.getId(), user3.getId()));

        assertThat(users).extracting("id")
            .isEqualTo(userIdsDesc(user1, user3));
    }

    @Test
    void returnsEmptyListIfNoUsersMatchOnMultiplePredicates() {
        user1 = minimalUserAccount();
        user1.setEmailAddress("some-user-email");
        user2 = minimalUserAccount();
        user3 = minimalUserAccount();
        dartsDatabase.saveAll(user1, user2, user3);

        var users = userManagementQuery.getUsers(user1.getEmailAddress(), List.of(user3.getId()));

        assertThat(users).isEmpty();
    }

    private static List<Integer> userIdsDesc(UserAccountEntity... users) {
        return Stream.of(users)
            .map(UserAccountEntity::getId)
            .sorted((u1, u2) -> u2 - u1)
            .toList();
    }
}