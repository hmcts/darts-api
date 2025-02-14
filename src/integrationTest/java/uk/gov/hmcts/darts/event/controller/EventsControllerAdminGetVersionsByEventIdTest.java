package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.Problem;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class EventsControllerAdminGetVersionsByEventIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @MockitoBean
    private EventDispatcher eventDispatcher;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private EventStub eventStub;

    private static final OffsetDateTime EVENT_TS = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void adminEventsApiGetVersionsByEventIdEndpointSuccess() throws Exception {

        // Given
        Map<Integer, List<EventEntity>> eventEntityVersions = eventStub.generateEventIdEventsIncludingZeroEventId(2, 2, false, EVENT_TS);
        EventEntity currentEventEntity = eventEntityVersions.get(1).get(0);

        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/" + currentEventEntity.getId() + "/versions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        AdminGetVersionsByEventIdResponseResult responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                                                 AdminGetVersionsByEventIdResponseResult.class);

        // Then
        Assertions.assertEquals(currentEventEntity.getId(), responseResult.getCurrentVersion().getId());
        Assertions.assertEquals(currentEventEntity.getLegacyObjectId(), responseResult.getCurrentVersion().getDocumentumId());
        Assertions.assertEquals(currentEventEntity.getEventId(), responseResult.getCurrentVersion().getSourceId());
        Assertions.assertEquals(currentEventEntity.getMessageId(), responseResult.getCurrentVersion().getMessageId());
        Assertions.assertEquals(currentEventEntity.getEventText(), responseResult.getCurrentVersion().getText());
        Assertions.assertEquals(currentEventEntity.getEventType().getId(), responseResult.getCurrentVersion().getEventMapping().getId());
        Assertions.assertEquals(currentEventEntity.getEventType().getEventName(), responseResult.getCurrentVersion().getEventMapping().getName());
        Assertions.assertEquals(currentEventEntity.isLogEntry(), responseResult.getCurrentVersion().getIsLogEntry());
        Assertions.assertEquals(currentEventEntity.getCourtroom().getId(), responseResult.getCurrentVersion().getCourtroom().getId());
        Assertions.assertEquals(currentEventEntity.getCourtroom().getName(), responseResult.getCurrentVersion().getCourtroom().getName());
        Assertions.assertEquals(currentEventEntity.getCourtroom().getCourthouse().getId(), responseResult.getCurrentVersion().getCourthouse().getId());
        Assertions.assertEquals(currentEventEntity.getCourtroom().getCourthouse().getDisplayName(),
                                responseResult.getCurrentVersion().getCourthouse().getDisplayName());
        Assertions.assertEquals(currentEventEntity.getLegacyVersionLabel(), responseResult.getCurrentVersion().getVersion());
        Assertions.assertEquals(currentEventEntity.getTimestamp(), responseResult.getCurrentVersion().getEventTs());
        Assertions.assertEquals(currentEventEntity.getIsCurrent(), responseResult.getCurrentVersion().getIsCurrent());
        Assertions.assertEquals(currentEventEntity.getCreatedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                                responseResult.getCurrentVersion().getCreatedAt());
        Assertions.assertEquals(currentEventEntity.getCreatedBy().getId(), responseResult.getCurrentVersion().getCreatedBy());
        Assertions.assertEquals(currentEventEntity.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                                responseResult.getCurrentVersion().getLastModifiedAt());
        Assertions.assertEquals(currentEventEntity.getLastModifiedBy().getId(), responseResult.getCurrentVersion().getLastModifiedBy());

        // Previous version
        EventEntity previousEventEntity = eventEntityVersions.get(1).get(1);
        Assertions.assertEquals(1, responseResult.getPreviousVersions().size());
        Assertions.assertEquals(previousEventEntity.getId(), responseResult.getPreviousVersions().getFirst().getId());
        Assertions.assertEquals(previousEventEntity.getLegacyObjectId(), responseResult.getPreviousVersions().getFirst().getDocumentumId());
        Assertions.assertEquals(previousEventEntity.getEventId(), responseResult.getPreviousVersions().getFirst().getSourceId());
        Assertions.assertEquals(previousEventEntity.getMessageId(), responseResult.getPreviousVersions().getFirst().getMessageId());
        Assertions.assertEquals(previousEventEntity.getEventText(), responseResult.getPreviousVersions().getFirst().getText());
        Assertions.assertEquals(previousEventEntity.getEventType().getId(), responseResult.getPreviousVersions().getFirst().getEventMapping().getId());
        Assertions.assertEquals(previousEventEntity.getEventType().getEventName(),
                                responseResult.getPreviousVersions().getFirst().getEventMapping().getName());
        Assertions.assertEquals(previousEventEntity.isLogEntry(), responseResult.getPreviousVersions().getFirst().getIsLogEntry());
        Assertions.assertEquals(previousEventEntity.getCourtroom().getId(), responseResult.getPreviousVersions().getFirst().getCourtroom().getId());
        Assertions.assertEquals(previousEventEntity.getCourtroom().getName(), responseResult.getPreviousVersions().getFirst().getCourtroom().getName());
        Assertions.assertEquals(previousEventEntity.getCourtroom().getCourthouse().getId(),
                                responseResult.getPreviousVersions().getFirst().getCourthouse().getId());
        Assertions.assertEquals(previousEventEntity.getCourtroom().getCourthouse().getDisplayName(),
                                responseResult.getPreviousVersions().getFirst().getCourthouse().getDisplayName());
        Assertions.assertEquals(previousEventEntity.getLegacyVersionLabel(), responseResult.getPreviousVersions().getFirst().getVersion());
        Assertions.assertEquals(previousEventEntity.getTimestamp(), responseResult.getPreviousVersions().getFirst().getEventTs());
        Assertions.assertEquals(previousEventEntity.getIsCurrent(), responseResult.getPreviousVersions().getFirst().getIsCurrent());
        Assertions.assertEquals(previousEventEntity.getCreatedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                                responseResult.getPreviousVersions().getFirst().getCreatedAt());
        Assertions.assertEquals(previousEventEntity.getCreatedBy().getId(), responseResult.getPreviousVersions().getFirst().getCreatedBy());
        Assertions.assertEquals(previousEventEntity.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                                responseResult.getPreviousVersions().getFirst().getLastModifiedAt());
        Assertions.assertEquals(previousEventEntity.getLastModifiedBy().getId(), responseResult.getPreviousVersions().getFirst().getLastModifiedBy());

        // When
        EventEntity eventIdZeroEventEntity = eventEntityVersions.get(0).getFirst();
        MockHttpServletRequestBuilder requestBuilder2 = get("/admin/events/" + eventIdZeroEventEntity.getId() + "/versions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response2 = mockMvc.perform(requestBuilder2).andExpect(status().is2xxSuccessful()).andReturn();

        AdminGetVersionsByEventIdResponseResult responseResult2 = objectMapper.readValue(response2.getResponse().getContentAsString(),
                                                                                        AdminGetVersionsByEventIdResponseResult.class);

        // Then
        Assertions.assertNull(responseResult2.getPreviousVersions());
        Assertions.assertEquals(eventIdZeroEventEntity.getId(), responseResult2.getCurrentVersion().getId());
    }

    @Test
    void adminEventsApiGetVersionsByEventIdEndpointFailureNoEventFound() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/-1/versions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();

        Problem responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                        Problem.class);

        // Then
        Assertions.assertEquals(CommonApiError.NOT_FOUND.getType(), responseResult.getType());
    }

}