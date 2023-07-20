package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.event.component.DartsEventMapper;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.CourtLogsService;
import uk.gov.hmcts.darts.event.service.EventDispatcher;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventsController.class)
@ActiveProfiles("intTest")
class EventsControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private EventDispatcher eventDispatcher;

    @MockBean
    private DartsEventMapper dartsEventMapper;

    @Test
    void eventsApiPostEndpoint() throws Exception {
        String requestBody = """
            {
              "message_id": "18422",
              "type": "1000",
              "sub_type": "1002",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": [
                "A20230049"
              ],
              "date_time": "2023-06-14T08:37:30.945Z"
            }""";

        String expectedResponse = """
            {
              "code": "201",
              "message": "CREATED"
            }""";
        MockHttpServletRequestBuilder requestBuilder = post("/events")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        assertEquals(expectedResponse, response.getResponse().getContentAsString(), NON_EXTENSIBLE);

        verify(eventDispatcher).receive(any(DartsEvent.class));
    }

}
