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
    void eventsApiPostEndpoint() throws Exception {
        String requestBody = "{\n" +
            "  \"messageId\": \"18422\",\n" +
            "  \"type\": \"10100\",\n" +
            "  \"subType\": \"10100\",\n" +
            "  \"courthouse\": \"SNARESBROOK\",\n" +
            "  \"courtroom\": \"1\",\n" +
            "  \"caseNumbers\": [\n" +
            "    \"A20230049\"\n" +
            "  ],\n" +
            "  \"dateTime\": \"2023-06-14T08:37:30.945Z\"\n" +
            "}";
        MockHttpServletRequestBuilder requestBuilder = post("/events")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

}
