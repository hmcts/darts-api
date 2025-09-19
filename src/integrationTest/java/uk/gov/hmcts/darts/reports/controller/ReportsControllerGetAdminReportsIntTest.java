package uk.gov.hmcts.darts.reports.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ReportsControllerGetAdminReportsIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/reports";

    @Autowired
    private transient MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.INCLUDE)
    void getReportsAdmin_shouldReturn501Error_NotYetImplemented(SecurityRoleEnum role) throws Exception {

        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_URL))
            .andExpect(status().isNotImplemented())
            .andReturn();

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EXCLUDE)
    void getReportsAdmin_shouldReturnForbidden_ForNotAuthorisedUsers(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_URL))
            .andExpect(status().isForbidden())
            .andReturn();

        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals(
            """
                {
                  "type": "AUTHORISATION_109",
                  "title": "User is not authorised for this endpoint",
                  "status": 403
                }
                """, jsonString, JSONCompareMode.STRICT);
    }
}
