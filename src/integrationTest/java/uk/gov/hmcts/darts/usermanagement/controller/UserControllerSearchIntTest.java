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
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;

import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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

}
