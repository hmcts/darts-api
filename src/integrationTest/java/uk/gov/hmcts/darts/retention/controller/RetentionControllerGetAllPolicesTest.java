package uk.gov.hmcts.darts.retention.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class RetentionControllerGetAllPolicesTest extends IntegrationBase {
    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Test
    void shouldGetRetentionPoliciesForSuperAdmin() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        var requestBuilder = get(URI.create("/admin/retention-policy-types"));

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String expectedResponse = getContentsFromFile(
            "tests/retention/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, response.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void shouldFailToGetRetentionPoliciesForNonSuperAdmin() throws Exception {

        var requestBuilder = get(URI.create("/admin/retention-policy-types"));

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());


    }



}

