package uk.gov.hmcts.darts.transcriptions.controller;

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

import java.net.URI;

import static java.lang.Boolean.TRUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class TranscriptionControllerGetTranscriberTranscriptsWithTranscriberStatusOnlyIntTest {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions/transcriber-view");
    private static final String USER_ID_HEADER = "user_id";
    private static final String ASSIGNED_QUERY_PARAM = "assigned";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTranscriberTranscriptsShouldReturnTranscriptRequestsWithTranscriberStatusOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -10
            )
            .queryParam(ASSIGNED_QUERY_PARAM, TRUE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
              {
                "transcription_id": 81,
                "case_id": -1,
                "case_number": "T20231009-1",
                "courthouse_name": "Bristol",
                "hearing_date": "2023-11-17",
                "transcription_type": "Specified Times",
                "status": "With Transcriber",
                "urgency": "Standard",
                "requested_ts": "2023-11-23T17:45:14.940936Z",
                "state_change_ts": "2023-11-23T17:45:51.151621Z",
                "is_manual": true
              }
            ]
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}
