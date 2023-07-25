package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseControllerGetCaseHearingsTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String pathVariable = "25";
    private static String endpointUrl = "/cases/" + pathVariable + "/hearings";

    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl);
        mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented());
    }

}
