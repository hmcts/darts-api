package uk.gov.hmcts.darts.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Isolated
class SecurityGroupControllerIntTest extends IntegrationBase {

    public static final String TEST_COURTHOUSE_NAME = "SWANSEA";
    public static final Integer MID_TIER_GROUP_ID = -17;
    public static final String ADMIN_SECURITY_GROUPS_ENDPOINT_URL = "/admin/security-groups";
    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @Autowired
    private UserAccountStub userAccountStub;
    @MockitoBean
    private UserIdentity userIdentity;
    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void getSecurityGroupsShouldSucceedAndReturnAllGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualJson = mvcResult.getResponse().getContentAsString();

        String expected = TestUtils.getContentsFromFile(
            "tests/usermanagement/SecurityGroupControllerGetTest/securityGroupsGetEndpointAllReturned.json");

        JSONAssert.assertEquals(expected, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterByRoleIdShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("role_ids", "3")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
                [
                  {
                    "user_ids":[],
                    "id":-1,
                    "security_role_id":3,
                    "global_access":false,
                    "display_state":true,
                    "display_name":"Test Approver",
                    "courthouse_ids":[],
                    "name":"Test Approver"
                  }
                ]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterByRoleIdsShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("role_ids", "1,6")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
             [{
                  "courthouse_ids": [],
                  "display_name": "Test Judge Global",
                  "display_state": true,
                  "global_access": true,
                  "id": -7,
                  "name": "Test Judge Global",
                  "security_role_id": 1,
                  "user_ids": []
              }, {
                  "courthouse_ids": [],
                  "display_name": "Test Judge",
                  "display_state": true,
                  "global_access": false,
                  "id": -3,
                  "name": "Test Judge",
                  "security_role_id": 1,
                  "user_ids": []
              }, {
                  "courthouse_ids": [],
                  "display_name": "Test RCJ Appeals",
                  "display_state": true,
                  "global_access": true,
                  "id": -6,
                  "name": "Test RCJ Appeals",
                  "security_role_id": 6,
                  "user_ids": []
              }]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterByRoleIdWithNoMatchShouldSucceedAndReturnEmptyArray() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("role_ids", "100")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsByCourthouseIdShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var courthouseEntity = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME);
        addCourthouseToSecurityGroup(courthouseEntity, MID_TIER_GROUP_ID);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("courthouse_id", courthouseEntity.getId().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = String.format("""
                                                    [
                                                      {
                                                        "user_ids":[],
                                                        "id":-17,
                                                        "security_role_id":14,
                                                        "global_access":true,
                                                        "display_state":false,
                                                        "display_name":"Mid Tier Group",
                                                        "courthouse_ids":[%s],
                                                        "name":"Mid Tier Group"
                                                      }
                                                    ]
                                                """, courthouseEntity.getId().toString());

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsByCourthouseIdWithNoMatchShouldSucceedAndReturnEmptyArray() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("courthouse_id", "500")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsByRoleIdsAndCourthouseIdShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var courthouseEntity = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME);
        addCourthouseToSecurityGroup(courthouseEntity, MID_TIER_GROUP_ID);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("role_ids", "6,9,14")
            .queryParam("courthouse_id", courthouseEntity.getId().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = String.format("""
                                                    [
                                                      {
                                                        "user_ids":[],
                                                        "id":-17,
                                                        "security_role_id":14,
                                                        "global_access":true,
                                                        "display_state":false,
                                                        "display_name": "Mid Tier Group",
                                                        "courthouse_ids":[%s],
                                                        "name":"Mid Tier Group"
                                                      }
                                                    ]
                                                """, courthouseEntity.getId().toString());

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterByUserIdShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("user_id", "-46")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
               [{
                   "courthouse_ids": [],
                   "display_name": "Cpp Group",
                   "display_state": false,
                   "global_access": true,
                   "id": -15,
                   "name": "Cpp Group",
                   "security_role_id": 12,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Dar Pc Group",
                   "display_state": false,
                   "global_access": true,
                   "id": -16,
                   "name": "Dar Pc Group",
                   "security_role_id": 13,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Mid Tier Group",
                   "display_state": false,
                   "global_access": true,
                   "id": -17,
                   "name": "Mid Tier Group",
                   "security_role_id": 14,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Super Admin",
                   "display_state": true,
                   "global_access": true,
                   "id": 1,
                   "name": "SUPER_ADMIN",
                   "security_role_id": 8,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Super User",
                   "display_state": true,
                   "global_access": true,
                   "id": 2,
                   "name": "SUPER_USER",
                   "security_role_id": 7,
                   "user_ids": []
               }]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterBySingleUsersShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("singleton_user", "true")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
                [{
                    "courthouse_ids": [],
                    "display_name": "Cpp Group",
                    "display_state": false,
                    "global_access": true,
                    "id": -15,
                    "name": "Cpp Group",
                    "security_role_id": 12,
                    "user_ids": []
                }, {
                    "courthouse_ids": [],
                    "display_name": "Dar Pc Group",
                    "display_state": false,
                    "global_access": true,
                    "id": -16,
                    "name": "Dar Pc Group",
                    "security_role_id": 13,
                    "user_ids": []
                }, {
                    "courthouse_ids": [],
                    "display_name": "Mid Tier Group",
                    "display_state": false,
                    "global_access": true,
                    "id": -17,
                    "name": "Mid Tier Group",
                    "security_role_id": 14,
                    "user_ids": []
                }, {
                    "courthouse_ids": [],
                    "display_name": "Super User",
                    "display_state": true,
                    "global_access": true,
                    "id": 2,
                    "name": "SUPER_USER",
                    "security_role_id": 7,
                    "user_ids": []
                }]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterByMultiUsersShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("singleton_user", "false")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
                [
                    {
                        "user_ids":[],
                        "id":1,
                        "security_role_id":8,
                        "global_access":true,
                        "display_state":true,
                        "courthouse_ids":[],
                        "name":"SUPER_ADMIN",
                        "display_name":"Super Admin"
                    }
                ]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsFilterByUserIdAndSingleUsersShouldSucceedAndReturnFilteredGroups() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("user_id", "-46")
            .queryParam("singleton_user", "true")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
               [{
                   "courthouse_ids": [],
                   "display_name": "Cpp Group",
                   "display_state": false,
                   "global_access": true,
                   "id": -15,
                   "name": "Cpp Group",
                   "security_role_id": 12,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Dar Pc Group",
                   "display_state": false,
                   "global_access": true,
                   "id": -16,
                   "name": "Dar Pc Group",
                   "security_role_id": 13,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Mid Tier Group",
                   "display_state": false,
                   "global_access": true,
                   "id": -17,
                   "name": "Mid Tier Group",
                   "security_role_id": 14,
                   "user_ids": []
               }, {
                   "courthouse_ids": [],
                   "display_name": "Super User",
                   "display_state": true,
                   "global_access": true,
                   "id": 2,
                   "name": "SUPER_USER",
                   "security_role_id": 7,
                   "user_ids": []
               }]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getSecurityGroupsByCourthouseIdShouldSucceedAndReturnFilteredGroupsWithoutSystemUsers() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var courthouseEntity = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME);
        addCourthouseToSecurityGroup(courthouseEntity, -4);

        // add 2 users - system/non-system. only expect to see non-system in response
        List<UserAccountEntity> users = userAccountStub.createAuthorisedIntegrationTestUsersSystemAndNonSystem(courthouseEntity);
        UserAccountEntity nonSystemUser = users.stream().filter(userAccount -> !userAccount.getIsSystemUser()).findFirst().orElseThrow();

        MockHttpServletRequestBuilder requestBuilder = get(ADMIN_SECURITY_GROUPS_ENDPOINT_URL)
            .queryParam("courthouse_id", courthouseEntity.getId().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = String.format("""
                                                    [
                                                      {
                                                        "user_ids":[%s],
                                                        "id":-4,
                                                        "security_role_id":4,
                                                        "global_access":false,
                                                        "display_state":true,
                                                        "display_name": "Test Transcriber",
                                                        "courthouse_ids":[%s],
                                                        "name":"Test Transcriber"
                                                      }
                                                    ]
                                                """, nonSystemUser.getId(), courthouseEntity.getId());

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    private void addCourthouseToSecurityGroup(CourthouseEntity courthouseEntity, Integer securityGroupId) {

        var securityGroupEntity = dartsDatabase.getSecurityGroupRepository().findById(securityGroupId);

        if (securityGroupEntity.isPresent()) {
            var securityGroup = securityGroupEntity.get();
            securityGroup.setCourthouseEntities(Set.of(courthouseEntity));
            dartsDatabase.getSecurityGroupRepository().save(securityGroup);
        }
    }

}
