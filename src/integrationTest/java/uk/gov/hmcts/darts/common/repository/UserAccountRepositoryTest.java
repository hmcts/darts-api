package uk.gov.hmcts.darts.common.repository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

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
            .emailAddress("some-user-email")
            .build()
            .getEntity();
        dartsPersistence.save(user1);

        userAccountEntity1 = user1;

        UserAccountEntity user2 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some-user-email-2")
            .build()
            .getEntity();
        dartsPersistence.save(user2);

        userAccountEntity2 = user2;

        UserAccountEntity user3 = PersistableFactory.getUserAccountTestData().someMinimalBuilder()
            .emailAddress("some-user-email-3")
            .isSystemUser(true)
            .build()
            .getEntity();
        dartsPersistence.save(user3);

        userAccountEntity3 = user3;

    }

    @Test
    void findUsers_shouldReturnUsers_WhenEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnUser_WhenIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity1.getId()));
    }

    @Test
    void findUsers_shouldReturnUser_WhenEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, userAccountEntity1.getEmailAddress(), null, null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getEmailAddress(), equalTo(userAccountEntity1.getEmailAddress()));
    }

    @Test
    void findUsers_shouldReturnMultipleUsers_WhenIDsProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, null, List.of(userAccountEntity1.getId(), userAccountEntity2.getId()), null
        );
        assertThat(users, Matchers.hasSize(2));
        UserAccountEntity firstFoundUser = users.get(0);
        UserAccountEntity secondFoundUser = users.get(1);
        assertThat(firstFoundUser.getId(), equalTo(userAccountEntity1.getId()));
        assertThat(secondFoundUser.getId(), equalTo(userAccountEntity2.getId()));
    }

    @Test
    void findUsers_shouldNotReturnUsers_WhenNonMatchingEmailProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            false, "non-matching-email", null, null
        );

        assertThat(users, Matchers.hasSize(0));
    }

    @Test
    void findUsers_shouldNotReturnUsers_WhenNonMatchingIdProvided() {
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
            -99  // migration user added by flyway
        ));
    }

    @Test
    void findUsers_shouldReturnSystemUser_WhenIdProvided() {
        List<UserAccountEntity> users = userAccountRepository.findUsers(
            true, null, List.of(userAccountEntity3.getId()), null
        );

        assertThat(users, Matchers.hasSize(1));
        assertThat(users.get(0).getId(), equalTo(userAccountEntity3.getId()));
    }
}
