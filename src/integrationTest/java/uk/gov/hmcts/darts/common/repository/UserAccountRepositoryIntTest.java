package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAccountRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private AuthorisationApi authorisationApi;

    @MockitoBean
    private UserIdentity userIdentity;

    private UserAccountEntity integrationTestUser;

    @BeforeEach
    void setUp() {
        integrationTestUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        Mockito.when(authorisationApi.getCurrentUser()).thenReturn(integrationTestUser);
    }

    @Test
    void findByEmailAddressIgnoreCase_shouldReturnExpectedUserAccount_whenItExistsIgnoringCase() {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        String userEmail = "inteGrationTest.user@EXample.com";

        List<UserAccountEntity> foundUsers = userAccountRepository.findByEmailAddressIgnoreCase(userEmail);

        assertEquals(1, foundUsers.size());
        assertEquals(integrationTestUser.getEmailAddress(), foundUsers.get(0).getEmailAddress());
    }
}
