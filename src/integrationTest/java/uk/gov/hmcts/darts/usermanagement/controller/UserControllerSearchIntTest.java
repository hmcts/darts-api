package uk.gov.hmcts.darts.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;

import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@AutoConfigureMockMvc
class UserControllerSearchIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/users/search";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;
    @Autowired
    private UserAccountStub userAccountStub;

    @Test
    void searchShouldReturnForbiddenError() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(false);

        UserSearch userSearch = new UserSearch();
        userSearch.setEmailAddress("@example");

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isForbidden())
            .andReturn();

        String expectedResponse = """
            {"type":"AUTHORISATION_107","title":"Failed to check authorisation","status":403}
            """;
        JSONAssert.assertEquals(
            expectedResponse,
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void searchShouldReturnBadRequestError() throws Exception {
        UserSearch userSearch = new UserSearch();
        userSearch.setEmailAddress("");

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isBadRequest())
            .andReturn();

        String expectedResponse = """
            {
              "violations": [
                {
                  "field": "emailAddress",
                  "message": "size must be between 1 and 256"
                }
              ],
              "type": "https://zalando.github.io/problem/constraint-violation",
              "status": 400,
              "title": "Constraint Violation"
            }
            """;
        JSONAssert.assertEquals(
            expectedResponse,
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );

        verifyNoInteractions(mockUserIdentity);
    }

    @Test
    void searchShouldReturnOk() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        UserSearch userSearch = new UserSearch();
        userSearch.setEmailAddress("@test");

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andReturn();

        String expectedResponse = "[]";
        JSONAssert.assertEquals(
            expectedResponse,
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void searchByEmailAddressShouldReturnOk() throws Exception {
        UserAccountEntity testUser = userAccountStub.createUnauthorisedIntegrationTestUser();
        SecurityGroupEntity testTranscriberSG = dartsDatabaseStub.getSecurityGroupRepository().getReferenceById(-4);
        testUser.getSecurityGroupEntities().add(testTranscriberSG);
        testUser.setActive(true);
        dartsDatabaseStub.getUserAccountRepository().save(testUser);

        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        UserSearch userSearch = new UserSearch();
        userSearch.setEmailAddress("integrationtest.user@");

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].full_name").value("IntegrationTest User"))
            .andExpect(jsonPath("$[0].email_address").value("integrationtest.user@example.com"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].security_groups").isArray())
            .andExpect(jsonPath("$[0].security_groups", hasSize(1)))
            .andExpect(jsonPath("$[0].security_groups", hasItem(-4)));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void searchByFullNameShouldReturnOk() throws Exception {
        UserAccountEntity testUser = userAccountStub.createUnauthorisedIntegrationTestUser();
        SecurityGroupEntity testTranscriberSG = dartsDatabaseStub.getSecurityGroupRepository().getReferenceById(-4);
        testUser.getSecurityGroupEntities().add(testTranscriberSG);
        testUser.setActive(true);
        dartsDatabaseStub.getUserAccountRepository().save(testUser);

        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        UserSearch userSearch = new UserSearch();
        userSearch.setFullName("IntegrationTest");

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].full_name").value("IntegrationTest User"))
            .andExpect(jsonPath("$[0].email_address").value("integrationtest.user@example.com"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].security_groups").isArray())
            .andExpect(jsonPath("$[0].security_groups", hasSize(1)))
            .andExpect(jsonPath("$[0].security_groups", hasItem(-4)));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void searchByEmailAddressAndFullNameShouldReturnOk() throws Exception {
        UserAccountEntity testUser = userAccountStub.createUnauthorisedIntegrationTestUser();
        SecurityGroupEntity testTranscriberSG = dartsDatabaseStub.getSecurityGroupRepository().getReferenceById(-4);
        testUser.getSecurityGroupEntities().add(testTranscriberSG);
        testUser.setActive(true);
        dartsDatabaseStub.getUserAccountRepository().save(testUser);

        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        UserSearch userSearch = new UserSearch();
        userSearch.setEmailAddress("integrationtest.user@");
        userSearch.setFullName("IntegrationTest");

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].full_name").value("IntegrationTest User"))
            .andExpect(jsonPath("$[0].email_address").value("integrationtest.user@example.com"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].security_groups").isArray())
            .andExpect(jsonPath("$[0].security_groups", hasSize(1)))
            .andExpect(jsonPath("$[0].security_groups", hasItem(-4)));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void searchByEmailAddressAndFullNameShouldReturnOkWhenUserInactive() throws Exception {
        UserAccountEntity testUser = userAccountStub.createUnauthorisedIntegrationTestUser();
        SecurityGroupEntity testTranscriberSG = dartsDatabaseStub.getSecurityGroupRepository().getReferenceById(-4);
        testUser.getSecurityGroupEntities().add(testTranscriberSG);
        testUser.setActive(false);
        dartsDatabaseStub.getUserAccountRepository().save(testUser);

        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        UserSearch userSearch = new UserSearch();
        userSearch.setEmailAddress("integrationtest.user@");
        userSearch.setFullName("IntegrationTest");

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].full_name").value("IntegrationTest User"))
            .andExpect(jsonPath("$[0].email_address").value("integrationtest.user@example.com"))
            .andExpect(jsonPath("$[0].active").value(false))
            .andExpect(jsonPath("$[0].security_groups").isArray())
            .andExpect(jsonPath("$[0].security_groups", hasSize(1)))
            .andExpect(jsonPath("$[0].security_groups", hasItem(-4)));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void returnsActiveAndInactiveUsersGivenActiveValueNotProvided() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        String randomStr = randomAlphabetic(4);
        String username1 = randomStr + "-user-1";
        String username2 = randomStr + "-user-2";

        UserAccountEntity activeUser = activeUserWithName(username1);
        UserAccountEntity inactiveUser = inactiveUserWithName(username2);
        dartsDatabaseStub.saveAll(activeUser, inactiveUser);

        UserSearch userSearch = new UserSearch();
        userSearch.setFullName(randomStr + "-user");

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].full_name").value(containsInAnyOrder(activeUser.getUserName(), inactiveUser.getUserName())))
            .andExpect(jsonPath("$[*].email_address").value(containsInAnyOrder(username1 + "@ex.com", username2 + "@ex.com")));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);

        dartsDatabaseStub.addToUserAccountTrash(username1 + "@ex.com", username2 + "@ex.com");
    }

    @Test
    void doesntReturnActiveUsersWhenInactiveProvidedInTheUserSearch() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        String randomStr = randomAlphabetic(4);
        String username1 = randomStr + "-user-1";
        String username2 = randomStr + "-user-2";

        UserAccountEntity activeUser = activeUserWithName(username1);
        UserAccountEntity inactiveUser = inactiveUserWithName(username2);
        dartsDatabaseStub.saveAll(activeUser, inactiveUser);

        UserSearch userSearch = new UserSearch();
        userSearch.setFullName(randomStr + "-user");
        userSearch.setActive(false);

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].full_name").value(hasItems(inactiveUser.getUserName())))
            .andExpect(jsonPath("$[*].full_name").value(hasSize(1)))
            .andExpect(jsonPath("$[*].email_address").value(hasItems(username2 + "@ex.com")));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);

        dartsDatabaseStub.addToUserAccountTrash(username1 + "@ex.com", username2 + "@ex.com");
    }

    @Test
    void doesntReturnInactiveUsersWhenActiveProvidedInTheUserSearch() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(ADMIN))).thenReturn(true);

        String randomStr = randomAlphabetic(4);
        String username1 = randomStr + "-user-1";
        String username2 = randomStr + "-user-2";

        UserAccountEntity activeUser = activeUserWithName(username1);
        UserAccountEntity inactiveUser = inactiveUserWithName(username2);
        dartsDatabaseStub.saveAll(activeUser, inactiveUser);

        UserSearch userSearch = new UserSearch();
        userSearch.setFullName(randomStr + "-user");
        userSearch.setActive(true);

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(userSearch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].full_name").value(hasItems(activeUser.getUserName())))
            .andExpect(jsonPath("$[*].full_name").value(hasSize(1)))
            .andExpect(jsonPath("$[*].email_address").value(hasItems(username1 + "@ex.com")));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);

        dartsDatabaseStub.addToUserAccountTrash(username1 + "@ex.com", username2 + "@ex.com");
    }

    private UserAccountEntity activeUserWithName(String name) {
        var user = userWithName(name);
        user.setActive(true);
        return user;
    }

    private UserAccountEntity inactiveUserWithName(String name) {
        var user = userWithName(name);
        user.setActive(false);
        return user;
    }

    private UserAccountEntity userWithName(String name) {
        var testTranscriberSG = dartsDatabaseStub.getSecurityGroupRepository().getReferenceById(-4);
        var user = new UserAccountEntity();
        user.setUserName(name);
        user.setEmailAddress(name + "@ex.com");
        user.getSecurityGroupEntities().add(testTranscriberSG);
        user.setAccountGuid(UUID.randomUUID().toString());
        user.setIsSystemUser(false);
        return user;
    }
}
