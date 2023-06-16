package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.enums.DarNotifyType;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.event.service.EventsService;
import uk.gov.hmcts.darts.event.service.impl.EventsServiceImpl;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventsController.class)
@Import(EventsServiceImpl.class)
@ActiveProfiles("intTest")
class EventsControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private EventsService eventsService;

    @MockBean
    private DartsGatewayClient mockDartsGatewayClient;

    private static final String NOTIFICATION_TYPE = DarNotifyType.CASE_UPDATE.getNotificationType();

    @Test
    void eventsApiPostEndpoint() throws Exception {
        String requestBody = "{\n" +
            "  \"message_id\": \"18422\",\n" +
            "  \"type\": \"10100\",\n" +
            "  \"sub_type\": \"10100\",\n" +
            "  \"courthouse\": \"SNARESBROOK\",\n" +
            "  \"courtroom\": \"1\",\n" +
            "  \"case_numbers\": [\n" +
            "    \"A20230049\"\n" +
            "  ],\n" +
            "  \"date_time\": \"2023-06-14T08:37:30.945Z\"\n" +
            "}";
        MockHttpServletRequestBuilder requestBuilder = post("/events")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");

        DarNotifyEvent expectedDarNotifyEvent = DarNotifyEvent.builder()
            .notificationType(NOTIFICATION_TYPE)
            .timestamp(OffsetDateTime.parse("2023-06-14T08:37:30.945Z"))
            .courthouse("SNARESBROOK")
            .courtroom("1")
            .caseNumbers(List.of("A20230049"))
            .build();
        Mockito.verify(mockDartsGatewayClient).notifyEvent(eq(expectedDarNotifyEvent));
        Mockito.verifyNoMoreInteractions(mockDartsGatewayClient);
    }

}
