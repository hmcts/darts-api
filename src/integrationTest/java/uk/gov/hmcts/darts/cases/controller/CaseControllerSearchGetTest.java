package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.DATE_FROM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.ENDPOINT_URL;

@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseControllerSearchGetTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL)
            .queryParam(COURTHOUSE, "SWANSEA")
            .queryParam(COURTROOM, "1")
            .queryParam(DATE_FROM, "2022-05-20");
        mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented());
    }

}
