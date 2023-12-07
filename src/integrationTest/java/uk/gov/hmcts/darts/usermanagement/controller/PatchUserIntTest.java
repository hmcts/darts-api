package uk.gov.hmcts.darts.usermanagement.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PatchUserIntTest extends IntegrationBase {

    private static final String ORIGINAL_USERNAME = "James Smith";
    private static final String ORIGINAL_EMAIL_ADDRESS = "james.smith@hmcts.net";
    private static final String ORIGINAL_DESCRIPTION = "A test user";
    private static final int ORIGINAL_SECURITY_GROUP_ID_1 = -1;
    private static final int ORIGINAL_SECURITY_GROUP_ID_2 = -2;
    private static final boolean ORIGINAL_SYSTEM_USER_FLAG = false;
    private static final OffsetDateTime ORIGINAL_LAST_LOGIN_TIME = OffsetDateTime.parse("2023-04-11T15:37:59Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private AuthorisationApi authorisationApi;

    private TransactionTemplate transactionTemplate;
    private UserAccountEntity integrationTestUser;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        integrationTestUser = dartsDatabase.getUserAccountStub()
            .getIntegrationTestUserAccountEntity();
        Mockito.when(authorisationApi.getCurrentUser())
            .thenReturn(integrationTestUser);
    }

    @Test
    void patchUserShouldSucceedWhenProvidedWithValidValueForSubsetOfAllowableFields() throws Exception {
        UserAccountEntity existingAccount = createEnabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "full_name": "Jimmy Smith"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value("Jimmy Smith"))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.last_login").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_groups", Matchers.containsInAnyOrder(
                ORIGINAL_SECURITY_GROUP_ID_1,
                ORIGINAL_SECURITY_GROUP_ID_2
            )));

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals("Jimmy Smith", latestUserAccountEntity.getUserName());
            assertEquals(ORIGINAL_EMAIL_ADDRESS, latestUserAccountEntity.getEmailAddress());
            assertEquals(ORIGINAL_DESCRIPTION, latestUserAccountEntity.getUserDescription());
            assertEquals(true, latestUserAccountEntity.isActive());
            assertThat(
                getSecurityGroupIds(latestUserAccountEntity),
                hasItems(ORIGINAL_SECURITY_GROUP_ID_1, ORIGINAL_SECURITY_GROUP_ID_2)
            );
            assertEquals(ORIGINAL_SYSTEM_USER_FLAG, latestUserAccountEntity.getIsSystemUser());

            assertEquals(existingAccount.getCreatedDateTime(), latestUserAccountEntity.getCreatedDateTime());
            assertThat(latestUserAccountEntity.getLastModifiedDateTime(), greaterThan(existingAccount.getLastModifiedDateTime()));
            assertEquals(ORIGINAL_LAST_LOGIN_TIME, latestUserAccountEntity.getLastLoginTime());
            assertEquals(integrationTestUser.getId(), latestUserAccountEntity.getLastModifiedBy().getId());
            assertEquals(integrationTestUser.getId(), latestUserAccountEntity.getCreatedBy().getId());

            return null;
        });
    }

    @Test
    void patchUserShouldSucceedWhenProvidedWithValidValuesForAllAllowableFields() throws Exception {
        UserAccountEntity existingAccount = createEnabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "full_name": "Jimmy Smith",
                           "description": "An updated description",
                           "active": false,
                           "security_groups": [ ]
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value("Jimmy Smith"))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value("An updated description"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.last_login").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_groups").isEmpty());

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals("Jimmy Smith", latestUserAccountEntity.getUserName());
            assertEquals(ORIGINAL_EMAIL_ADDRESS, latestUserAccountEntity.getEmailAddress());
            assertEquals("An updated description", latestUserAccountEntity.getUserDescription());
            assertEquals(false, latestUserAccountEntity.isActive());
            assertThat(getSecurityGroupIds(latestUserAccountEntity), empty()
            );
            assertEquals(ORIGINAL_SYSTEM_USER_FLAG, latestUserAccountEntity.getIsSystemUser());

            assertEquals(existingAccount.getCreatedDateTime(), latestUserAccountEntity.getCreatedDateTime());
            assertThat(latestUserAccountEntity.getLastModifiedDateTime(), greaterThan(existingAccount.getLastModifiedDateTime()));
            assertEquals(ORIGINAL_LAST_LOGIN_TIME, latestUserAccountEntity.getLastLoginTime());
            assertEquals(integrationTestUser.getId(), latestUserAccountEntity.getLastModifiedBy().getId());
            assertEquals(integrationTestUser.getId(), latestUserAccountEntity.getCreatedBy().getId());

            return null;
        });
    }

    @Test
    void patchUserShouldFailIfChangeWithInvalidDataIsAttempted() throws Exception {
        UserAccountEntity existingAccount = createEnabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "full_name": " ",
                           "description": ""
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.violations[*].field", hasItems("fullName", "description")));
    }

    @Test
    void patchUserShouldFailIfProvidedUserIdDoesNotExistInDB() throws Exception {
        MockHttpServletRequestBuilder request = buildRequest(818_231)
            .content("""
                         {
                           "full_name": "Jimmy Smith"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("USER_MANAGEMENT_100"));
    }

    @Test
    void patchUserShouldSucceedAndClearSecurityGroupsWhenAccountGetsDisabledAndNoSecurityGroupsAreExplicitlyProvided() throws Exception {
        UserAccountEntity existingAccount = createEnabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "active": false
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.last_login").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_groups").isEmpty());

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();

            assertEquals(false, latestUserAccountEntity.isActive());
            assertThat(getSecurityGroupIds(latestUserAccountEntity), empty());

            return null;
        });
    }

    @Test
    void patchUserShouldSucceedWhenAccountGetsEnabled() throws Exception {
        UserAccountEntity existingAccount = createDisabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "active": true
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.last_login").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_groups").isEmpty());

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();

            assertEquals(true, latestUserAccountEntity.isActive());
            assertThat(getSecurityGroupIds(latestUserAccountEntity), empty());

            return null;
        });
    }

    @Test
    void patchUserShouldSucceedWhenSecurityGroupsAreUpdated() throws Exception {
        UserAccountEntity existingAccount = createEnabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "security_groups": [ -3, -4 ]
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.last_login").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_groups", not(Matchers.containsInAnyOrder(
                ORIGINAL_SECURITY_GROUP_ID_1,
                ORIGINAL_SECURITY_GROUP_ID_2
            ))))
            .andExpect(jsonPath("$.security_groups", Matchers.containsInAnyOrder(-3, -4)));

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();

            assertThat(
                getSecurityGroupIds(latestUserAccountEntity),
                not(hasItems(ORIGINAL_SECURITY_GROUP_ID_1, ORIGINAL_SECURITY_GROUP_ID_2))
            );
            assertThat(
                getSecurityGroupIds(latestUserAccountEntity),
                hasItems(-3, -4)
            );

            return null;
        });
    }

    @Test
    void patchUserShouldFailIfEmailAddressChangeIsAttemptedAndDataShouldRemainUnchanged() throws Exception {
        UserAccountEntity existingAccount = createEnabledUserAccountEntity();
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "email_address": "jimmy.smith@hmcts.net"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail", containsString("Unrecognized field \"email_address\"")));

        UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
            .findById(userId)
            .orElseThrow();
        assertEquals(ORIGINAL_SYSTEM_USER_FLAG, latestUserAccountEntity.getIsSystemUser());
        assertEquals(ORIGINAL_USERNAME, latestUserAccountEntity.getUserName());
        assertEquals(ORIGINAL_EMAIL_ADDRESS, latestUserAccountEntity.getEmailAddress());
        assertEquals(ORIGINAL_DESCRIPTION, latestUserAccountEntity.getUserDescription());
        assertEquals(true, latestUserAccountEntity.isActive());
        assertEquals(ORIGINAL_LAST_LOGIN_TIME, latestUserAccountEntity.getLastLoginTime());
    }

    private UserAccountEntity createEnabledUserAccountEntity() {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserName(ORIGINAL_USERNAME);
        userAccountEntity.setEmailAddress(ORIGINAL_EMAIL_ADDRESS);
        userAccountEntity.setUserDescription(ORIGINAL_DESCRIPTION);
        userAccountEntity.setActive(true);
        userAccountEntity.setLastLoginTime(ORIGINAL_LAST_LOGIN_TIME);

        userAccountEntity.setIsSystemUser(ORIGINAL_SYSTEM_USER_FLAG);
        userAccountEntity.setCreatedBy(integrationTestUser);
        userAccountEntity.setLastModifiedBy(integrationTestUser);

        SecurityGroupEntity securityGroupEntity1 = dartsDatabase.getSecurityGroupRepository()
            .getReferenceById(ORIGINAL_SECURITY_GROUP_ID_1);
        SecurityGroupEntity securityGroupEntity2 = dartsDatabase.getSecurityGroupRepository()
            .getReferenceById(ORIGINAL_SECURITY_GROUP_ID_2);
        userAccountEntity.setSecurityGroupEntities(Set.of(securityGroupEntity1, securityGroupEntity2));

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }

    private UserAccountEntity createDisabledUserAccountEntity() {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserName(ORIGINAL_USERNAME);
        userAccountEntity.setEmailAddress(ORIGINAL_EMAIL_ADDRESS);
        userAccountEntity.setUserDescription(ORIGINAL_DESCRIPTION);
        userAccountEntity.setActive(false);
        userAccountEntity.setLastLoginTime(ORIGINAL_LAST_LOGIN_TIME);

        userAccountEntity.setIsSystemUser(ORIGINAL_SYSTEM_USER_FLAG);
        userAccountEntity.setCreatedBy(integrationTestUser);
        userAccountEntity.setLastModifiedBy(integrationTestUser);

        userAccountEntity.setSecurityGroupEntities(Collections.emptySet());

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }

    private MockHttpServletRequestBuilder buildRequest(int userId) {
        return patch("/users/" + userId)
            .header("Content-Type", "application/json");
    }

    private static List<Integer> getSecurityGroupIds(UserAccountEntity createdUserAccountEntity) {
        return createdUserAccountEntity.getSecurityGroupEntities().stream()
            .map(SecurityGroupEntity::getId)
            .toList();
    }

}
