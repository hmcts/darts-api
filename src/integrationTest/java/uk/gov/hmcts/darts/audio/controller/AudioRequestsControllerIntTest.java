package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static java.lang.Boolean.FALSE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioRequestsControllerIntTest extends IntegrationBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getYourAudio() throws Exception {

        dartsDatabase.createAndLoadMediaRequestEntity();

        var requestBuilder = get(URI.create(String.format("/audio-requests?user_id=%d&expired=%s", -2, FALSE)));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [
                {
                    "media_request_id": 1,
                    "case_number": "2",
                    "courthouse_name": "some-courthouse",
                    "media_request_start_ts": "2023-06-26T13:00:00Z",
                    "media_request_end_ts": "2023-06-26T13:45:00Z",
                    "expiry_ts": "2023-06-26T13:45:00Z",
                    "media_request_status": "OPEN"
                }
            ]
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

}
