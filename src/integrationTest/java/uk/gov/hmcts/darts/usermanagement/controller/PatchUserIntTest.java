package uk.gov.hmcts.darts.usermanagement.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.Problem;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
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
    private static final boolean ORIGINAL_ACTIVE_STATE = true;
    private static final int ORIGINAL_SECURITY_GROUP_ID_1 = -1;
    private static final int ORIGINAL_SECURITY_GROUP_ID_2 = -2;
    private static final boolean ORIGINAL_SYSTEM_USER_FLAG = false;
    private static final OffsetDateTime ORIGINAL_LAST_LOGIN_TIME = OffsetDateTime.parse("2023-04-11T15:37:59Z");

    private static final String SECOND_EMAIL_ADDRESS = "jimmy.smith@hmcts.net";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private UserAccountStub accountStub;

    @MockitoBean
    private UserIdentity userIdentity;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void patchUserShouldSucceedWhenProvidedWithValidValueForSubsetOfAllowableFields() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
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
            .andExpect(jsonPath("$.active").value(ORIGINAL_ACTIVE_STATE))
            .andExpect(jsonPath("$.last_login_at").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_group_ids", Matchers.containsInAnyOrder(
                ORIGINAL_SECURITY_GROUP_ID_1,
                ORIGINAL_SECURITY_GROUP_ID_2
            )));

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals("Jimmy Smith", latestUserAccountEntity.getUserFullName());
            assertEquals(ORIGINAL_EMAIL_ADDRESS, latestUserAccountEntity.getEmailAddress());
            assertEquals(ORIGINAL_DESCRIPTION, latestUserAccountEntity.getUserDescription());
            assertEquals(ORIGINAL_ACTIVE_STATE, latestUserAccountEntity.isActive());
            assertThat(
                getSecurityGroupIds(latestUserAccountEntity),
                hasItems(ORIGINAL_SECURITY_GROUP_ID_1, ORIGINAL_SECURITY_GROUP_ID_2)
            );
            assertEquals(ORIGINAL_SYSTEM_USER_FLAG, latestUserAccountEntity.getIsSystemUser());

            assertThat(latestUserAccountEntity.getLastModifiedDateTime(), greaterThan(existingAccount.getLastModifiedDateTime()));
            assertEquals(ORIGINAL_LAST_LOGIN_TIME, latestUserAccountEntity.getLastLoginTime());
            assertEquals(user.getId(), latestUserAccountEntity.getLastModifiedById());
            assertEquals(user.getId(), latestUserAccountEntity.getCreatedById());

            return null;
        });
    }

    @Test
    void patchUserShouldSucceedWhenProvidedWithValidValuesForAllAllowableFields() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "full_name": "Jimmy Smith",
                           "description": "An updated description",
                           "active": false,
                           "security_group_ids": [ ]
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value("Jimmy Smith"))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value("An updated description"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.last_login_at").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_group_ids").isEmpty());

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals("Jimmy Smith", latestUserAccountEntity.getUserFullName());
            assertEquals(ORIGINAL_EMAIL_ADDRESS, latestUserAccountEntity.getEmailAddress());
            assertEquals("An updated description", latestUserAccountEntity.getUserDescription());
            assertEquals(false, latestUserAccountEntity.isActive());
            assertThat(getSecurityGroupIds(latestUserAccountEntity), empty());
            assertEquals(ORIGINAL_SYSTEM_USER_FLAG, latestUserAccountEntity.getIsSystemUser());

            assertEquals(existingAccount.getCreatedDateTime().toInstant(), latestUserAccountEntity.getCreatedDateTime().toInstant());
            assertThat(latestUserAccountEntity.getLastModifiedDateTime(), greaterThan(existingAccount.getLastModifiedDateTime()));
            assertEquals(ORIGINAL_LAST_LOGIN_TIME, latestUserAccountEntity.getLastLoginTime());
            assertEquals(user.getId(), latestUserAccountEntity.getLastModifiedById());
            assertEquals(user.getId(), latestUserAccountEntity.getCreatedById());

            return null;
        });
    }

    // Regression test to cover bug DMP-2459
    @Test
    void patchUserShouldSucceedWhenAnExistingDescriptionIsRemoved() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "description": ""
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(""))
            .andExpect(jsonPath("$.active").value(ORIGINAL_ACTIVE_STATE))
            .andExpect(jsonPath("$.last_login_at").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_group_ids", Matchers.containsInAnyOrder(
                ORIGINAL_SECURITY_GROUP_ID_1,
                ORIGINAL_SECURITY_GROUP_ID_2
            )));

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals(ORIGINAL_USERNAME, latestUserAccountEntity.getUserFullName());
            assertEquals(ORIGINAL_EMAIL_ADDRESS, latestUserAccountEntity.getEmailAddress());
            assertEquals("", latestUserAccountEntity.getUserDescription());
            assertEquals(ORIGINAL_ACTIVE_STATE, latestUserAccountEntity.isActive());
            assertThat(
                getSecurityGroupIds(latestUserAccountEntity),
                hasItems(ORIGINAL_SECURITY_GROUP_ID_1, ORIGINAL_SECURITY_GROUP_ID_2)
            );
            assertEquals(ORIGINAL_SYSTEM_USER_FLAG, latestUserAccountEntity.getIsSystemUser());

            assertEquals(ORIGINAL_LAST_LOGIN_TIME, latestUserAccountEntity.getLastLoginTime());
            assertEquals(user.getId(), latestUserAccountEntity.getLastModifiedById());
            assertEquals(user.getId(), latestUserAccountEntity.getCreatedById());

            return null;
        });
    }

    @Test
    void patchUserShouldFailIfChangeWithInvalidDataIsAttempted() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "full_name": ""
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.violations[*].field", hasItems("fullName")));
    }

    @Test
    void patchUserShouldFailIfProvidedUserIdDoesNotExistInDB() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

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
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
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
            .andExpect(jsonPath("$.last_login_at").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_group_ids").isEmpty());

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
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createDisabledUserAccountEntity(user);
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
            .andExpect(jsonPath("$.active").value(ORIGINAL_ACTIVE_STATE))
            .andExpect(jsonPath("$.last_login_at").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_group_ids").isEmpty());

        transactionTemplate.execute(status -> {
            UserAccountEntity latestUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();

            assertEquals(ORIGINAL_ACTIVE_STATE, latestUserAccountEntity.isActive());
            assertThat(getSecurityGroupIds(latestUserAccountEntity), empty());

            return null;
        });
    }

    @Test
    void patchUserShouldSucceedWhenSecurityGroupsAreUpdated() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "security_group_ids": [ -3, -4 ]
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(ORIGINAL_ACTIVE_STATE))
            .andExpect(jsonPath("$.last_login_at").value(ORIGINAL_LAST_LOGIN_TIME.toString()))
            .andExpect(jsonPath("$.security_group_ids", not(Matchers.containsInAnyOrder(
                ORIGINAL_SECURITY_GROUP_ID_1,
                ORIGINAL_SECURITY_GROUP_ID_2
            ))))
            .andExpect(jsonPath("$.security_group_ids", Matchers.containsInAnyOrder(-3, -4)));

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
    void patchUserShouldSucceedIfEmailAddressChangeDifferent() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "email_address": "jimmy.nail@hmcts.net"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value("jimmy.nail@hmcts.net"));


    }

    @Test
    void patchUserSameEmailShouldBeOkAndDataShouldRemainUnchanged() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "email_address": "james.smith@hmcts.net"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$.email_address").value(ORIGINAL_EMAIL_ADDRESS));
    }

    @Test
    void patchUserDuplicateEmailShouldFail() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        createEnabledUserAccountEntity(user);
        UserAccountEntity secondAccount = createEnabledUserAccountEntity(user, SECOND_EMAIL_ADDRESS);
        Integer userId = secondAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "email_address": "james.smith@hmcts.net"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("USER_MANAGEMENT_101"))
            .andExpect(jsonPath("$.title").value("The provided email already exists"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("User with email james.smith@hmcts.net already exists"));
    }

    @Test
    void patchUserShouldFailIfUserIsNotAuthorised() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsNotAuthorised(userIdentity);

        UserAccountEntity existingAccount = createEnabledUserAccountEntity(user);
        Integer userId = existingAccount.getId();

        MockHttpServletRequestBuilder request = buildRequest(userId)
            .content("""
                         {
                           "full_name": "Jimmy Smith"
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void patchUserShouldFailIfProvidedUserIsASystemUser() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = accountStub.getSystemUserAccountEntity();

        MockHttpServletRequestBuilder request = buildRequest(userAccountEntity.getId())
            .content("""
                         {
                           "full_name": "Jimmy Smith"
                         }
                         """);

        MvcResult mvcResult = mockMvc.perform(request).andReturn();

        Problem problem = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), Problem.class);
        assertEquals(UserManagementError.USER_NOT_FOUND.getHttpStatus().value(), mvcResult.getResponse().getStatus());
        assertEquals(UserManagementError.USER_NOT_FOUND.getErrorTypeNumeric(), problem.getType().toString());
    }

    private UserAccountEntity createEnabledUserAccountEntity(UserAccountEntity user, String email) {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserFullName(ORIGINAL_USERNAME);
        userAccountEntity.setEmailAddress(email);
        userAccountEntity.setUserDescription(ORIGINAL_DESCRIPTION);
        userAccountEntity.setActive(ORIGINAL_ACTIVE_STATE);
        userAccountEntity.setLastLoginTime(ORIGINAL_LAST_LOGIN_TIME);

        userAccountEntity.setIsSystemUser(ORIGINAL_SYSTEM_USER_FLAG);
        userAccountEntity.setCreatedBy(user);
        userAccountEntity.setLastModifiedBy(user);

        SecurityGroupEntity securityGroupEntity1 = dartsDatabase.getSecurityGroupRepository()
            .getReferenceById(ORIGINAL_SECURITY_GROUP_ID_1);
        SecurityGroupEntity securityGroupEntity2 = dartsDatabase.getSecurityGroupRepository()
            .getReferenceById(ORIGINAL_SECURITY_GROUP_ID_2);
        userAccountEntity.setSecurityGroupEntities(Set.of(securityGroupEntity1, securityGroupEntity2));

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }

    private UserAccountEntity createEnabledUserAccountEntity(UserAccountEntity user) {
        return createEnabledUserAccountEntity(user, ORIGINAL_EMAIL_ADDRESS);
    }

    private UserAccountEntity createDisabledUserAccountEntity(UserAccountEntity user) {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserFullName(ORIGINAL_USERNAME);
        userAccountEntity.setEmailAddress(ORIGINAL_EMAIL_ADDRESS);
        userAccountEntity.setUserDescription(ORIGINAL_DESCRIPTION);
        userAccountEntity.setActive(false);
        userAccountEntity.setLastLoginTime(ORIGINAL_LAST_LOGIN_TIME);

        userAccountEntity.setIsSystemUser(ORIGINAL_SYSTEM_USER_FLAG);
        userAccountEntity.setCreatedBy(user);
        userAccountEntity.setLastModifiedBy(user);

        userAccountEntity.setSecurityGroupEntities(Collections.emptySet());

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }

    private MockHttpServletRequestBuilder buildRequest(int userId) {
        return patch("/admin/users/" + userId)
            .header("Content-Type", "application/json");
    }

    private static List<Integer> getSecurityGroupIds(UserAccountEntity createdUserAccountEntity) {
        return createdUserAccountEntity.getSecurityGroupEntities().stream()
            .map(SecurityGroupEntity::getId)
            .toList();
    }

}
