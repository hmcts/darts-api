package uk.gov.hmcts.darts.retention.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class RetentionControllerGetPolicyByIdTest extends IntegrationBase {

    static final int POLICY_TYPE_ID = 1;

    static final String RETENTION_POLICY_TYPE_URL = "/admin/retention-policy-types/{id}";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Test
    void shouldGetRetentionPoliciesForSuperAdmin() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        var requestBuilder = get(RETENTION_POLICY_TYPE_URL,POLICY_TYPE_ID);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String expectedResponse = getContentsFromFile(
            "tests/retention/expectedResponsePolicyTypeId.json");
        JSONAssert.assertEquals(expectedResponse, response.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void shouldFailToGetRetentionPoliciesForNonSuperAdmin() throws Exception {

        var requestBuilder = get(RETENTION_POLICY_TYPE_URL,POLICY_TYPE_ID);

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());


    }

    @Test
    void shouldFailToGetRetentionPoliciesForInvalidId() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        var requestBuilder = get(RETENTION_POLICY_TYPE_URL, 45);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("RETENTION_108")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(404)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("The retention policy type id does not exist.")));
    }
}