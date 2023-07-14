package uk.gov.hmcts.darts.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.event.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class EventsControllerCourtLogsTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/courtlogs");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void courtLogsPostShouldReturnNotImplementedError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsBytes(createRequestBody()));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotImplemented());
    }

    @Test
    void courtLogsPostShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json");

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    private CourtLogsPostRequestBody createRequestBody() {
        return new CourtLogsPostRequestBody(OffsetDateTime.now(),
                                            "some-courthouse",
                                            "some-courtroom",
                                            Collections.singletonList("some-case-number"),
                                            "some-text");
    }

}
