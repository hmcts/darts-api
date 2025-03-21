package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperUserStub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;


@AutoConfigureMockMvc
class TranscriptionControllerGetAllStatusTest extends IntegrationBase {

    private static final String ENDPOINT_URL_TRANSCRIPTION = "/admin/transcription-status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private SuperUserStub superUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Test
    void shouldSuccessfullyReturnTranscriptionStatusesForSuperAdmin() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription_status/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldSuccessfullyReturnTranscriptionStatusesForSuperUser() throws Exception {

        superUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription_status/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    void shouldFailToReturnTranscriptionStatusesForNonSuperAdminOrSuperUser() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION);

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andReturn();
    }


}

