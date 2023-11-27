package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.transcriptions.config.ClockTestConfiguration;

import java.net.URI;
import java.time.Clock;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"test.clock.fixed-instant=2023-11-24T15:00:00Z"})
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Import(ClockTestConfiguration.class)
class TranscriptionControllerGetTranscriberTranscriptsIntTest {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions/transcriber-view");
    private static final String USER_ID_HEADER = "user_id";
    private static final String ASSIGNED_QUERY_PARAM = "assigned";

    @Autowired
    private Clock clock;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTranscriberTranscriptsShouldReturnBadRequestWhenMissingUserIdHeader() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .queryParam(ASSIGNED_QUERY_PARAM, TRUE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest()).andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Required request header 'user_id' for method parameter type Integer is not present"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriberTranscriptsShouldReturnBadRequestWhenMissingAssignedQueryParam() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -1
            );

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest()).andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Required request parameter 'assigned' for method parameter type Boolean is not present"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriberTranscriptsShouldReturnTranscriptRequestsOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -1
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = "[]";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriberTranscriptsShouldReturnTranscriberTranscriptionsWorkOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -1
            )
            .queryParam(ASSIGNED_QUERY_PARAM, TRUE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = "[]";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriberTranscriptsShouldReturnTranscriptRequestsWithApprovedStatusOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -10
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
              {
                "transcription_id": 41,
                "case_id": -1,
                "case_number": "T20231009-1",
                "courthouse_name": "Bristol",
                "hearing_date": "2023-11-17",
                "transcription_type": "Specified Times",
                "status": "Approved",
                "urgency": "Standard",
                "requested_ts": "2023-11-23T16:25:55.304517Z",
                "state_change_ts": "2023-11-23T16:26:20.441633Z",
                "is_manual": true
              }
            ]
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriberTranscriptsShouldReturnTranscriptRequestsWithTranscriberAndCompletedStatusOk() throws Exception {
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
                "transcription_id": 101,
                "case_id": -1,
                "case_number": "T20231009-1",
                "courthouse_name": "Bristol",
                "hearing_date": "2023-11-17",
                "transcription_type": "Specified Times",
                "status": "Complete",
                "urgency": "Standard",
                "requested_ts": "2023-11-24T12:37:00.812692Z",
                "state_change_ts": "2023-11-24T12:53:42.839577Z",
                "is_manual": true
              },
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
