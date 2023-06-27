package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.cases.service.CaseService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.DATE;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class CaseControllerTest {
    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private CaseService caseService;

    @Test
    void casesGetEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/cases")
            .queryParam(COURTHOUSE, "SWANSEA")
            .queryParam(COURTROOM, "1")
            .queryParam(DATE, "2023-06-20");
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesGetEndpoint/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }
}
