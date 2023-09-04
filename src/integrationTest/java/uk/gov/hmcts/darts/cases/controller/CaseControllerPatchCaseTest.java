package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CaseControllerPatchCaseTest extends IntegrationBase {

    public static final String ENDPOINT_URL = "/cases/{case_id}";
    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void testOk() throws Exception {
        CourtCaseEntity createdCase = dartsDatabase.createCase("testCourthouse", "testCaseNumber");

        MockHttpServletRequestBuilder requestBuilder = patch(ENDPOINT_URL, createdCase.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("""
                         {
                           "retain_until": "2023-09-06T16:16:57.331Z"
                         }""");
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"case_id":<<case_id>>,"courthouse":"testCourthouse","case_number":"testCaseNumber","retain_until":"2023-09-06T16:16:57.331Z"}""";
        expectedResponse = expectedResponse.replace("<<case_id>>", createdCase.getId().toString());
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testFail() throws Exception {
        CourtCaseEntity createdCase = dartsDatabase.createCase("testCourthouse", "testCaseNumber");

        MockHttpServletRequestBuilder requestBuilder = patch(ENDPOINT_URL, createdCase.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("""
                         {
                           "retain_until": ""
                         }""");
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"CASE_106","title":"The request does not contain any values that are supported by the PATCH operation.","status":400}""";
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}
