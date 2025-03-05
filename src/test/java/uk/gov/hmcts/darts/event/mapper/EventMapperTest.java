package uk.gov.hmcts.darts.event.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetails;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class EventMapperTest {

    private EventMapper eventMapper;

    @BeforeEach
    public void before() {
        eventMapper = new EventMapper();
    }

    @Test
    void mapsEventToAdminSearchEventResponseResult() {
        // When
        EventEntity eventEntity = setGenericEventDataForTest();
        eventEntity.setId(2);
        eventEntity.setCreatedDateTime(OffsetDateTime.now());
        eventEntity.setIsCurrent(true);

        // Given
        AdminGetEventResponseDetails responseResult = eventMapper.mapToAdminGetEventsResponseForId(eventEntity);

        // Then
        assertAdminEventResponseDetails(eventEntity, responseResult);
    }

    @Test
    void whenSingleVersionForAnEvent_mapsEventVersionToAdminGetEventVersionsResponseResult() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        EventEntity eventEntity1 = setGenericEventDataForTest();
        eventEntity1.setId(1);
        eventEntity1.setCreatedDateTime(now.minusDays(1));
        eventEntity1.setIsCurrent(true);

        List<EventEntity> eventEntities = List.of(eventEntity1);

        // Given
        AdminGetVersionsByEventIdResponseResult responseResult = eventMapper.mapToAdminGetEventVersionsResponseForId(eventEntities);

        AdminGetEventResponseDetails currentVersion = responseResult.getCurrentVersion();
        // Then
        assertAdminEventResponseDetails(eventEntity1, currentVersion);

        assertThat(responseResult.getPreviousVersions()).hasSize(0);
    }

    @Test
    @SuppressWarnings({"PMD.NcssCount"})
    void whenMultipleVersionsForAnEvent_mapsEventVersionToAdminGetEventVersionsResponseResult() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        EventEntity eventEntity1 = setGenericEventDataForTest();
        eventEntity1.setId(1);
        eventEntity1.setCreatedDateTime(now.minusDays(1));
        eventEntity1.setIsCurrent(true);

        EventEntity eventEntity2 = setGenericEventDataForTest();
        eventEntity2.setId(2);
        eventEntity2.setCreatedDateTime(now);
        eventEntity2.setIsCurrent(true);

        EventEntity eventEntity3 = setGenericEventDataForTest();
        eventEntity3.setId(3);
        eventEntity3.setCreatedDateTime(now.minusDays(2));
        eventEntity3.setIsCurrent(true);

        EventEntity eventEntity4 = setGenericEventDataForTest();
        eventEntity4.setId(4);
        eventEntity4.setCreatedDateTime(now);
        eventEntity4.setIsCurrent(false);

        List<EventEntity> eventEntities = List.of(eventEntity1, eventEntity2, eventEntity3, eventEntity4);

        // Given
        AdminGetVersionsByEventIdResponseResult responseResult = eventMapper.mapToAdminGetEventVersionsResponseForId(eventEntities);

        AdminGetEventResponseDetails currentVersion = responseResult.getCurrentVersion();
        // Then
        assertAdminEventResponseDetails(eventEntity2, currentVersion);

        List<AdminGetEventResponseDetails> previousVersions = responseResult.getPreviousVersions();
        assertEquals(3, previousVersions.size());

        assertAdminEventResponseDetails(eventEntity4, previousVersions.getFirst());

        assertAdminEventResponseDetails(eventEntity1, previousVersions.get(1));

        assertAdminEventResponseDetails(eventEntity3, previousVersions.get(2));
    }

    private static void assertAdminEventResponseDetails(EventEntity eventEntity2, AdminGetEventResponseDetails currentVersion) {
        assertEquals(eventEntity2.getId(), currentVersion.getId());
        assertEquals(eventEntity2.getLegacyObjectId(), currentVersion.getDocumentumId());
        assertEquals(eventEntity2.getEventId(), currentVersion.getSourceId());
        assertEquals(eventEntity2.getMessageId(), currentVersion.getMessageId());
        assertEquals(eventEntity2.getEventText(), currentVersion.getText());
        assertEquals(eventEntity2.getEventType().getId(), currentVersion.getEventMapping().getId());
        assertEquals(eventEntity2.isLogEntry(), currentVersion.getIsLogEntry());
        assertEquals(eventEntity2.getCourtroom().getId(), currentVersion.getCourtroom().getId());
        assertEquals(eventEntity2.getCourtroom().getName(), currentVersion.getCourtroom().getName());
        assertEquals(eventEntity2.getCourtroom().getCourthouse().getId(), currentVersion.getCourthouse().getId());
        assertEquals(eventEntity2.getCourtroom().getCourthouse().getDisplayName(), currentVersion.getCourthouse().getDisplayName());
        assertEquals(eventEntity2.getLegacyVersionLabel(), currentVersion.getVersion());
        assertEquals(eventEntity2.getChronicleId(), currentVersion.getChronicleId());
        assertEquals(eventEntity2.getAntecedentId(), currentVersion.getAntecedentId());
        assertEquals(eventEntity2.getTimestamp(), currentVersion.getEventTs());
        assertEquals(eventEntity2.getIsCurrent(), currentVersion.getIsCurrent());
        assertEquals(eventEntity2.getCreatedDateTime(), currentVersion.getCreatedAt());
        assertEquals(eventEntity2.getCreatedBy().getId(), currentVersion.getCreatedBy());
        assertEquals(eventEntity2.getLastModifiedDateTime(), currentVersion.getLastModifiedAt());
        assertEquals(eventEntity2.getLastModifiedBy().getId(), currentVersion.getLastModifiedBy());
        assertEquals(eventEntity2.isDataAnonymised(), currentVersion.getIsDataAnonymised());
    }

    @Test
    void whenSingleVersionForAnEventIsNotCurrent_mapsEventVersionToAdminGetEventVersionsResponseResultAndVersionSetInPreviousEvent() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        EventEntity eventEntity1 = setGenericEventDataForTest();
        eventEntity1.setId(1);
        eventEntity1.setCreatedDateTime(now.minusDays(1));
        eventEntity1.setIsCurrent(false);

        List<EventEntity> eventEntities = List.of(eventEntity1);

        // Given
        AdminGetVersionsByEventIdResponseResult responseResult = eventMapper.mapToAdminGetEventVersionsResponseForId(eventEntities);

        AdminGetEventResponseDetails previousVersion = responseResult.getPreviousVersions().get(0);
        // Then
        assertNull(responseResult.getCurrentVersion());
        assertAdminEventResponseDetails(eventEntity1, previousVersion);
    }

    private EventEntity setGenericEventDataForTest() {
        EventEntity eventEntity = new EventEntity();

        eventEntity.setEventId(200);
        eventEntity.setLegacyObjectId("legacyObjectId");
        eventEntity.setMessageId("messageId");
        eventEntity.setEventText("eventText");

        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setId(300);
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setLogEntry(false);

        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setId(500);
        courtroom.setName("courtroomName");

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(700);
        courthouseEntity.setDisplayName("courthouseDisplayName");
        courtroom.setCourthouse(courthouseEntity);

        eventEntity.setCourtroom(courtroom);
        eventEntity.setLegacyVersionLabel("legacyVersionLabel");
        eventEntity.setChronicleId("chronicleId");
        eventEntity.setAntecedentId("antecedentId");

        UserAccountEntity uaCreatedEntity = new UserAccountEntity();
        eventEntity.setCreatedBy(uaCreatedEntity);

        UserAccountEntity uaLastModifiedEntity = new UserAccountEntity();
        eventEntity.setLastModifiedDateTime(OffsetDateTime.now());
        eventEntity.setLastModifiedBy(uaLastModifiedEntity);
        eventEntity.setTimestamp(OffsetDateTime.now());

        return eventEntity;
    }
}