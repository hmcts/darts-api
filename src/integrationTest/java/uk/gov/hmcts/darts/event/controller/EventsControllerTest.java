package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.event.component.DartsEventMapper;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.Problem;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@AutoConfigureMockMvc
class EventsControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private EventDispatcher eventDispatcher;

    @MockBean
    private AudioApi audioApi;

    @MockBean
    private DartsEventMapper dartsEventMapper;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Test
    void eventsApiPostEndpoint() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(CPP);
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

    @Test
    void adminEventsApiGetByIdEndpointSuccess() throws Exception {

        // Given
        // setup an event id
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        LocalDateTime hearingDate = LocalDateTime.of(2020, 6, 6, 20, 0, 0);
        HearingEntity hearing = dartsDatabaseStub.createHearing("Courthouse", "1", "12345", hearingDate);
        EventEntity eventEntity = dartsDatabaseStub.createEvent(hearing);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/" + eventEntity.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        AdminGetEventForIdResponseResult responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                                                 AdminGetEventForIdResponseResult.class);

        // Then
        Assertions.assertEquals(eventEntity.getId(), responseResult.getId());
        Assertions.assertEquals(eventEntity.getLegacyObjectId(), responseResult.getDocumentumId());
        Assertions.assertEquals(eventEntity.getEventId(), responseResult.getSourceId());
        Assertions.assertEquals(eventEntity.getMessageId(), responseResult.getMessageId());
        Assertions.assertEquals(eventEntity.getEventText(), responseResult.getText());
        Assertions.assertEquals(eventEntity.getEventType().getId(), responseResult.getEventMapping().getId());
        Assertions.assertEquals(eventEntity.isLogEntry(), responseResult.getIsLogEntry());
        Assertions.assertEquals(eventEntity.getCourtroom().getId(), responseResult.getCourtroom().getId());
        Assertions.assertEquals(eventEntity.getCourtroom().getName(), responseResult.getCourtroom().getName());
        Assertions.assertEquals(eventEntity.getCourtroom().getCourthouse().getId(), responseResult.getCourthouse().getId());
        Assertions.assertEquals(eventEntity.getCourtroom().getCourthouse().getDisplayName(), responseResult.getCourthouse().getDisplayName());
        Assertions.assertEquals(eventEntity.getLegacyVersionLabel(), responseResult.getVersion());
        Assertions.assertEquals(eventEntity.getChronicleId(), responseResult.getChronicleId());
        Assertions.assertEquals(eventEntity.getAntecedentId(), responseResult.getAntecedentId());
        Assertions.assertEquals(eventEntity.getCreatedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), responseResult.getCreatedAt());
        Assertions.assertEquals(eventEntity.getCreatedBy().getId(), responseResult.getCreatedBy());
        Assertions.assertEquals(eventEntity.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), responseResult.getLastModifiedAt());
        Assertions.assertEquals(eventEntity.getLastModifiedBy().getId(), responseResult.getLastModifiedBy());
    }

    @Test
    void adminEventsApiGetByIdEndpointFailureNoEventFound() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/-1")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();

        Problem responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                        Problem.class);

        // Then
        Assertions.assertEquals(EventError.EVENT_ID_NOT_FOUND_RESULTS.getType(), responseResult.getType());
    }

    @Test
    void adminEventsApiGetByIdEndpointFailureNotSuperAdmin() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_USER);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/-1")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        // Then
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andReturn();
    }
}