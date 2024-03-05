package uk.gov.hmcts.darts.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TestUtils;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityGroupControllerIntTest extends IntegrationBase {

    public static final String TEST_COURTHOUSE_NAME = "SWANSEA";
    public static final Integer MID_TIER_GROUP_ID = -17;
    public static final String ADMIN_SECURITY_GROUPS_ENDPOINT_URL = "/admin/security-groups";
    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @MockBean
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
            .queryParam("role_ids", "1")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
                [
                  {
                    "id":-1,
                    "security_role_id":1,
                    "global_access":false,
                    "display_state":true,
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
            .queryParam("role_ids", "3,6")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = """
                [
                   {
                     "id":-6,
                     "security_role_id":6,
                     "global_access":true,
                     "display_state":true,
                     "courthouse_ids":[],
                     "name":"Test RCJ Appeals"
                   },
                   {
                     "id":-3,
                     "security_role_id":3,
                     "global_access":false,
                     "display_state":true,
                     "courthouse_ids":[],
                     "name":"Test Judge"
                   }
                 ]
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
                    "id":-17,
                    "security_role_id":10,
                    "global_access":true,
                    "display_state":true,
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
            .queryParam("courthouse_id","500")
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
            .queryParam("role_ids", "6,9,10")
            .queryParam("courthouse_id",courthouseEntity.getId().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        String expectedJson = String.format("""
                [
                  {
                    "id":-17,
                    "security_role_id":10,
                    "global_access":true,
                    "display_state":true,
                    "courthouse_ids":[%s],
                    "name":"Mid Tier Group"
                  }
                ]
            """, courthouseEntity.getId().toString());

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
