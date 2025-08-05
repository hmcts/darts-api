package uk.gov.hmcts.darts.usermanagement.controller;

import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PostUserIntTest extends IntegrationBase {

    private static final String USERNAME = "James Smith";
    private static final String EMAIL_ADDRESS = "james.smith@hmcts.net";
    private static final String DESCRIPTION = "A test user";
    private static final int SECURITY_GROUP_ID_1 = -1;
    private static final int SECURITY_GROUP_ID_2 = -2;
    private static final boolean SYSTEM_USER_FLAG = false;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private AuthorisationApi authorisationApi;

    @MockitoBean
    private UserIdentity userIdentity;

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
    void createUserShouldSucceedWhenProvidedWithValidValuesForMinimumRequiredFields() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "full_name": "James Smith",
                           "email_address": "james.smith@hmcts.net"
                         }
                         """);

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.full_name").value(USERNAME))
            .andExpect(jsonPath("$.email_address").value(EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.last_login").doesNotExist())
            .andExpect(jsonPath("$.security_group_ids").isEmpty())
            .andReturn();

        int userId = getUserId(result);

        transactionTemplate.execute(status -> {
            UserAccountEntity createdUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals(USERNAME, createdUserAccountEntity.getUserFullName());
            assertEquals(EMAIL_ADDRESS, createdUserAccountEntity.getEmailAddress());
            assertNull(createdUserAccountEntity.getUserDescription());
            assertTrue(createdUserAccountEntity.isActive());
            assertTrue(createdUserAccountEntity.getSecurityGroupEntities().isEmpty());
            assertEquals(SYSTEM_USER_FLAG, createdUserAccountEntity.getIsSystemUser());

            assertNotNull(createdUserAccountEntity.getCreatedDateTime());
            assertNotNull(createdUserAccountEntity.getLastModifiedDateTime());
            assertNull(createdUserAccountEntity.getLastLoginTime());
            assertEquals(integrationTestUser.getId(), createdUserAccountEntity.getLastModifiedById());
            assertEquals(integrationTestUser.getId(), createdUserAccountEntity.getCreatedById());

            return null;
        });
    }

    @Test
    void createUserShouldSucceedWhenProvidedWithValidValuesForAllFields() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "full_name": "James Smith",
                           "email_address": "james.smith@hmcts.net",
                           "description": "A test user",
                           "active": true,
                           "security_group_ids": [
                             -1, -2
                           ]
                         }
                         """);
        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.full_name").value(USERNAME))
            .andExpect(jsonPath("$.email_address").value(EMAIL_ADDRESS))
            .andExpect(jsonPath("$.description").value(DESCRIPTION))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.last_login").doesNotExist())
            .andExpect(jsonPath("$.security_group_ids", Matchers.containsInAnyOrder(
                SECURITY_GROUP_ID_1,
                SECURITY_GROUP_ID_2
            )))
            .andReturn();

        int userId = getUserId(result);

        transactionTemplate.execute(status -> {
            UserAccountEntity createdUserAccountEntity = dartsDatabase.getUserAccountRepository()
                .findById(userId)
                .orElseThrow();
            assertEquals(USERNAME, createdUserAccountEntity.getUserFullName());
            assertEquals(EMAIL_ADDRESS, createdUserAccountEntity.getEmailAddress());
            assertEquals(DESCRIPTION, createdUserAccountEntity.getUserDescription());
            assertEquals(true, createdUserAccountEntity.isActive());
            assertThat(
                getSecurityGroupIds(createdUserAccountEntity),
                hasItems(SECURITY_GROUP_ID_1, SECURITY_GROUP_ID_2)
            );
            assertEquals(SYSTEM_USER_FLAG, createdUserAccountEntity.getIsSystemUser());

            assertNotNull(createdUserAccountEntity.getCreatedDateTime());
            assertNotNull(createdUserAccountEntity.getLastModifiedDateTime());
            assertNull(createdUserAccountEntity.getLastLoginTime());
            assertEquals(integrationTestUser.getId(), createdUserAccountEntity.getLastModifiedById());
            assertEquals(integrationTestUser.getId(), createdUserAccountEntity.getCreatedById());

            return null;
        });
    }

    @Test
    void createUserShouldFailWhenRequiredFieldsAreMissing() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .header("Content-Type", "application/json")
            .content("""
                         {
                           "description": "",
                           "active": true,
                           "security_group_ids": [
                             1
                           ]
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUserShouldFailWhenProvidedWithAUserEmailAddressThatMatchesAnExistingDisabledAccount() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserFullName("James Smith");
        userAccountEntity.setEmailAddress("james.smith@hmcts.net");
        userAccountEntity.setIsSystemUser(false);
        userAccountEntity.setActive(false);
        dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "full_name": "James Smith",
                           "email_address": "james.smith@hmcts.net"
                         }
                         """);
        MvcResult mvcResult = mockMvc.perform(request)
            .andExpect(status().isConflict()).andReturn();
        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(UserManagementError.DUPLICATE_EMAIL.getType(), problem.getType().toString());
        assertEquals(UserManagementError.DUPLICATE_EMAIL.getTitle(), problem.getTitle());
        assertEquals("User with email james.smith@hmcts.net already exists", problem.getDetail());
    }

    @Test
    void createUserShouldFailWhenProvidedWithASecurityGroupThatDoesntExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "full_name": "James Smith",
                           "email_address": "james.smith@hmcts.net",
                           "security_group_ids": [
                             9999999
                           ]
                         }
                         """);
        mockMvc.perform(request)
            .andExpect(status().isInternalServerError());
    }

    private MockHttpServletRequestBuilder buildRequest() {
        return post("/admin/users")
            .header("Content-Type", "application/json");
    }

    private int getUserId(MvcResult result) throws UnsupportedEncodingException {
        String contentAsString = result.getResponse().getContentAsString();
        return new JSONObject(contentAsString)
            .getInt("id");
    }

    private static List<Integer> getSecurityGroupIds(UserAccountEntity createdUserAccountEntity) {
        return createdUserAccountEntity.getSecurityGroupEntities().stream()
            .map(SecurityGroupEntity::getId)
            .toList();
    }

}
