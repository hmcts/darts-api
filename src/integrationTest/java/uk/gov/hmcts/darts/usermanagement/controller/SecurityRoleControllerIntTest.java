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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityRoleControllerIntTest extends IntegrationBase {

    public static final String ENDPOINT_URL = "/admin/security-roles";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void getSecurityRolesShouldSucceedAndReturnAllRoles() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
                [
                  {
                    "id": 1,
                    "display_name": "Approver",
                    "display_state": true
                  },
                  {
                    "id": 2,
                    "display_name": "Requestor",
                    "display_state": true
                  },
                  {
                    "id": 3,
                    "display_name": "Judge",
                    "display_state": true
                  },
                  {
                    "id": 4,
                    "display_name": "Transcriber",
                    "display_state": true
                  },
                  {
                    "id": 5,
                    "display_name": "Translation QA",
                    "display_state": true
                  },
                  {
                    "id": 6,
                    "display_name": "RCJ Appeals",
                    "display_state": true
                  },
                  {
                    "id": 7,
                    "display_name": "XHIBIT",
                    "display_state": true
                  },
                  {
                    "id": 8,
                    "display_name": "CPP",
                    "display_state": true
                  },
                  {
                    "id": 9,
                    "display_name": "DAR PC",
                    "display_state": true
                  },
                  {
                    "id": 10,
                    "display_name": "Mid Tier",
                    "display_state": true
                  },
                  {
                    "id": 11,
                    "display_name": "Super Admin",
                    "display_state": true
                  }
                ]
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getNonAdminSecurityRolesShouldThrowError() throws Exception {

        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        when(userIdentity.getUserAccount()).thenReturn(judgeUser);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        assertTrue(actualJson.contains("AUTHORISATION_109"));
        assertTrue(actualJson.contains("User is not authorised for this endpoint"));
    }

}
