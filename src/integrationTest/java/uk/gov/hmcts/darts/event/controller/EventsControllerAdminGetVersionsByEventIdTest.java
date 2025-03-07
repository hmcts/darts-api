package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.EventLinkedCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private EventLinkedCaseStub eventLinkedCaseStub;

    private static final OffsetDateTime EVENT_TS = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void adminEventsApiGetVersionsByEventIdEndpoint_shouldReturnCurrentEventAndPreviousEvents_whenEventIdIsNotZero() throws Exception {
        // Given
        Map<Integer, List<EventEntity>> eventEntityVersions = eventStub.generateEventIdEventsIncludingZeroEventId(2, 2, false, EVENT_TS);
        CourtCaseEntity courtCase = dartsDatabase.createCase("courthouse", "1234567890");
        linkToCaseNumber(eventEntityVersions.get(1), courtCase);
        EventEntity currentEventEntity = eventEntityVersions.get(1).get(0);

        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/" + currentEventEntity.getId() + "/versions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        AdminGetVersionsByEventIdResponseResult responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                                                        AdminGetVersionsByEventIdResponseResult.class);

        // Then
        assertEquals(currentEventEntity.getId(), responseResult.getCurrentVersion().getId());
        assertEquals(currentEventEntity.getLegacyObjectId(), responseResult.getCurrentVersion().getDocumentumId());
        assertEquals(currentEventEntity.getEventId(), responseResult.getCurrentVersion().getSourceId());
        assertEquals(currentEventEntity.getMessageId(), responseResult.getCurrentVersion().getMessageId());
        assertEquals(currentEventEntity.getEventText(), responseResult.getCurrentVersion().getText());
        assertEquals(currentEventEntity.getEventType().getId(), responseResult.getCurrentVersion().getEventMapping().getId());
        assertEquals(currentEventEntity.getEventType().getEventName(), responseResult.getCurrentVersion().getEventMapping().getName());
        assertEquals(currentEventEntity.isLogEntry(), responseResult.getCurrentVersion().getIsLogEntry());
        assertEquals(currentEventEntity.getCourtroom().getId(), responseResult.getCurrentVersion().getCourtroom().getId());
        assertEquals(currentEventEntity.getCourtroom().getName(), responseResult.getCurrentVersion().getCourtroom().getName());
        assertEquals(currentEventEntity.getCourtroom().getCourthouse().getId(), responseResult.getCurrentVersion().getCourthouse().getId());
        assertEquals(currentEventEntity.getCourtroom().getCourthouse().getDisplayName(),
                     responseResult.getCurrentVersion().getCourthouse().getDisplayName());
        assertEquals(currentEventEntity.getLegacyVersionLabel(), responseResult.getCurrentVersion().getVersion());
        assertEquals(currentEventEntity.getTimestamp(), responseResult.getCurrentVersion().getEventTs());
        assertEquals(currentEventEntity.getIsCurrent(), responseResult.getCurrentVersion().getIsCurrent());
        assertEquals(currentEventEntity.getCreatedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                     responseResult.getCurrentVersion().getCreatedAt());
        assertEquals(currentEventEntity.getCreatedBy().getId(), responseResult.getCurrentVersion().getCreatedBy());
        assertEquals(currentEventEntity.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                     responseResult.getCurrentVersion().getLastModifiedAt());
        assertEquals(currentEventEntity.getLastModifiedBy().getId(), responseResult.getCurrentVersion().getLastModifiedBy());

        // Previous version
        EventEntity previousEventEntity = eventEntityVersions.get(1).get(1);
        assertEquals(1, responseResult.getPreviousVersions().size());
        assertEquals(previousEventEntity.getId(), responseResult.getPreviousVersions().getFirst().getId());
        assertEquals(previousEventEntity.getLegacyObjectId(), responseResult.getPreviousVersions().getFirst().getDocumentumId());
        assertEquals(previousEventEntity.getEventId(), responseResult.getPreviousVersions().getFirst().getSourceId());
        assertEquals(previousEventEntity.getMessageId(), responseResult.getPreviousVersions().getFirst().getMessageId());
        assertEquals(previousEventEntity.getEventText(), responseResult.getPreviousVersions().getFirst().getText());
        assertEquals(previousEventEntity.getEventType().getId(), responseResult.getPreviousVersions().getFirst().getEventMapping().getId());
        assertEquals(previousEventEntity.getEventType().getEventName(),
                     responseResult.getPreviousVersions().getFirst().getEventMapping().getName());
        assertEquals(previousEventEntity.isLogEntry(), responseResult.getPreviousVersions().getFirst().getIsLogEntry());
        assertEquals(previousEventEntity.getCourtroom().getId(), responseResult.getPreviousVersions().getFirst().getCourtroom().getId());
        assertEquals(previousEventEntity.getCourtroom().getName(), responseResult.getPreviousVersions().getFirst().getCourtroom().getName());
        assertEquals(previousEventEntity.getCourtroom().getCourthouse().getId(),
                     responseResult.getPreviousVersions().getFirst().getCourthouse().getId());
        assertEquals(previousEventEntity.getCourtroom().getCourthouse().getDisplayName(),
                     responseResult.getPreviousVersions().getFirst().getCourthouse().getDisplayName());
        assertEquals(previousEventEntity.getLegacyVersionLabel(), responseResult.getPreviousVersions().getFirst().getVersion());
        assertEquals(previousEventEntity.getTimestamp(), responseResult.getPreviousVersions().getFirst().getEventTs());
        assertEquals(previousEventEntity.getIsCurrent(), responseResult.getPreviousVersions().getFirst().getIsCurrent());
        assertEquals(previousEventEntity.getCreatedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                     responseResult.getPreviousVersions().getFirst().getCreatedAt());
        assertEquals(previousEventEntity.getCreatedBy().getId(), responseResult.getPreviousVersions().getFirst().getCreatedBy());
        assertEquals(previousEventEntity.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                     responseResult.getPreviousVersions().getFirst().getLastModifiedAt());
        assertEquals(previousEventEntity.getLastModifiedBy().getId(), responseResult.getPreviousVersions().getFirst().getLastModifiedBy());
    }

    @Test
    void adminEventsApiGetVersionsByEventIdEndpoint_shouldReturnCurrentEventAndPreviousEventsOnlyForSameCaseNumbers() throws Exception {

        // Given
        Map<Integer, List<EventEntity>> eventEntityVersions = eventStub.generateEventIdEventsIncludingZeroEventId(2, 2, false, EVENT_TS);
        CourtCaseEntity courtCase = dartsDatabase.createCase("courthouse", "1234567890");
        linkToCaseNumber(eventEntityVersions.get(1), courtCase);

        EventEntity currentEventEntity = eventEntityVersions.get(1).get(0);

        // Generate another event with the same event id but different case number
        Map<Integer, List<EventEntity>> eventEntityVersions2 = eventStub.generateEventIdEventsIncludingZeroEventId(2, 2, false, EVENT_TS);
        eventEntityVersions2.get(1).forEach(eventEntity -> {
            eventEntity.setEventId(currentEventEntity.getEventId());
            eventStub.saveEvent(eventEntity);
        });
        CourtCaseEntity courtCase2 = dartsDatabase.createCase("courthouse", "new-case");
        linkToCaseNumber(eventEntityVersions.get(1), courtCase2);


        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        // When
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/" + currentEventEntity.getId() + "/versions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        AdminGetVersionsByEventIdResponseResult responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                                                        AdminGetVersionsByEventIdResponseResult.class);

        // Then
        assertEquals(currentEventEntity.getId(), responseResult.getCurrentVersion().getId());

        // Previous version
        EventEntity previousEventEntity = eventEntityVersions.get(1).get(1);
        assertEquals(1, responseResult.getPreviousVersions().size());
        assertEquals(previousEventEntity.getId(), responseResult.getPreviousVersions().getFirst().getId());
    }

    @Test
    void adminEventsApiGetVersionsByEventIdEndpoint_shouldReturnCurrentEventAndNoPreviousEvents_whenEventIdIsZero() throws Exception {

        // Given
        Map<Integer, List<EventEntity>> eventEntityVersions = eventStub.generateEventIdEventsIncludingZeroEventId(1, 2, false, EVENT_TS);
        CourtCaseEntity courtCase = dartsDatabase.createCase("courthouse", "1234567890");
        linkToCaseNumber(eventEntityVersions.get(0), courtCase);
        EventEntity currentEventEntity = eventEntityVersions.get(0).get(0);

        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        // When
        EventEntity eventIdZeroEventEntity = eventEntityVersions.get(0).getFirst();
        MockHttpServletRequestBuilder requestBuilder = get("/admin/events/" + eventIdZeroEventEntity.getId() + "/versions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        AdminGetVersionsByEventIdResponseResult responseResult = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                                                        AdminGetVersionsByEventIdResponseResult.class);

        // Then
        assertThat(responseResult.getPreviousVersions()).hasSize(0);

        assertEquals(currentEventEntity.getId(), responseResult.getCurrentVersion().getId());
        assertEquals(currentEventEntity.getLegacyObjectId(), responseResult.getCurrentVersion().getDocumentumId());
        assertEquals(currentEventEntity.getEventId(), responseResult.getCurrentVersion().getSourceId());
        assertEquals(currentEventEntity.getMessageId(), responseResult.getCurrentVersion().getMessageId());
        assertEquals(currentEventEntity.getEventText(), responseResult.getCurrentVersion().getText());
        assertEquals(currentEventEntity.getEventType().getId(), responseResult.getCurrentVersion().getEventMapping().getId());
        assertEquals(currentEventEntity.getEventType().getEventName(), responseResult.getCurrentVersion().getEventMapping().getName());
        assertEquals(currentEventEntity.isLogEntry(), responseResult.getCurrentVersion().getIsLogEntry());
        assertEquals(currentEventEntity.getCourtroom().getId(), responseResult.getCurrentVersion().getCourtroom().getId());
        assertEquals(currentEventEntity.getCourtroom().getName(), responseResult.getCurrentVersion().getCourtroom().getName());
        assertEquals(currentEventEntity.getCourtroom().getCourthouse().getId(), responseResult.getCurrentVersion().getCourthouse().getId());
        assertEquals(currentEventEntity.getCourtroom().getCourthouse().getDisplayName(),
                     responseResult.getCurrentVersion().getCourthouse().getDisplayName());
        assertEquals(currentEventEntity.getLegacyVersionLabel(), responseResult.getCurrentVersion().getVersion());
        assertEquals(currentEventEntity.getTimestamp(), responseResult.getCurrentVersion().getEventTs());
        assertEquals(currentEventEntity.getIsCurrent(), responseResult.getCurrentVersion().getIsCurrent());
        assertEquals(currentEventEntity.getCreatedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                     responseResult.getCurrentVersion().getCreatedAt());
        assertEquals(currentEventEntity.getCreatedBy().getId(), responseResult.getCurrentVersion().getCreatedBy());
        assertEquals(currentEventEntity.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
                     responseResult.getCurrentVersion().getLastModifiedAt());
        assertEquals(currentEventEntity.getLastModifiedBy().getId(), responseResult.getCurrentVersion().getLastModifiedBy());
    }

    @Test
    void adminEventsApiGetVersionsByEventIdEndpoint_shouldReturnError_whenEveIdCanNotBeFound() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(get("/admin/events/-1/versions")
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());
    }

    private void linkToCaseNumber(List<EventEntity> eventEntities, CourtCaseEntity caseEntity) {
        eventEntities.forEach(eventEntity -> {
            eventLinkedCaseStub.createCaseLinkedEvent(eventEntity, caseEntity);
        });
    }
}
