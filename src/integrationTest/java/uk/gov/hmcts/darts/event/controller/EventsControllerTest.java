package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ActiveProfiles("intTest")
@ContextConfiguration(classes = EventsController.class)
class EventsControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void eventsApiAddDocumentsEndpoint() throws Exception {
        String requestBody = "{}";
        MockHttpServletRequestBuilder requestBuilder = post("/event/addDocument")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("message_id", "1")
            .queryParam("type", "1")
            .queryParam("sub_type", "1")
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

}
