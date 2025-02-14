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
        assertEquals(eventEntity.getId(), responseResult.getId());
        assertEquals(eventEntity.getLegacyObjectId(), responseResult.getDocumentumId());
        assertEquals(eventEntity.getEventId(), responseResult.getSourceId());
        assertEquals(eventEntity.getMessageId(), responseResult.getMessageId());
        assertEquals(eventEntity.getEventText(), responseResult.getText());
        assertEquals(eventEntity.getEventType().getId(), responseResult.getEventMapping().getId());
        assertEquals(eventEntity.isLogEntry(), responseResult.getIsLogEntry());
        assertEquals(eventEntity.getCourtroom().getId(), responseResult.getCourtroom().getId());
        assertEquals(eventEntity.getCourtroom().getName(), responseResult.getCourtroom().getName());
        assertEquals(eventEntity.getCourtroom().getCourthouse().getId(), responseResult.getCourthouse().getId());
        assertEquals(eventEntity.getCourtroom().getCourthouse().getDisplayName(), responseResult.getCourthouse().getDisplayName());
        assertEquals(eventEntity.getLegacyVersionLabel(), responseResult.getVersion());
        assertEquals(eventEntity.getChronicleId(), responseResult.getChronicleId());
        assertEquals(eventEntity.getAntecedentId(), responseResult.getAntecedentId());
        assertEquals(eventEntity.getTimestamp(), responseResult.getEventTs());
        assertEquals(eventEntity.getIsCurrent(), responseResult.getIsCurrent());
        assertEquals(eventEntity.getCreatedDateTime(), responseResult.getCreatedAt());
        assertEquals(eventEntity.getCreatedBy().getId(), responseResult.getCreatedBy());
        assertEquals(eventEntity.getLastModifiedDateTime(), responseResult.getLastModifiedAt());
        assertEquals(eventEntity.getLastModifiedBy().getId(), responseResult.getLastModifiedBy());
        assertEquals(eventEntity.isDataAnonymised(), responseResult.getIsDataAnonymised());
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
        assertEquals(eventEntity1.getId(), currentVersion.getId());
        assertEquals(eventEntity1.getLegacyObjectId(), currentVersion.getDocumentumId());
        assertEquals(eventEntity1.getEventId(), currentVersion.getSourceId());
        assertEquals(eventEntity1.getMessageId(), currentVersion.getMessageId());
        assertEquals(eventEntity1.getEventText(), currentVersion.getText());
        assertEquals(eventEntity1.getEventType().getId(), currentVersion.getEventMapping().getId());
        assertEquals(eventEntity1.isLogEntry(), currentVersion.getIsLogEntry());
        assertEquals(eventEntity1.getCourtroom().getId(), currentVersion.getCourtroom().getId());
        assertEquals(eventEntity1.getCourtroom().getName(), currentVersion.getCourtroom().getName());
        assertEquals(eventEntity1.getCourtroom().getCourthouse().getId(), currentVersion.getCourthouse().getId());
        assertEquals(eventEntity1.getCourtroom().getCourthouse().getDisplayName(), currentVersion.getCourthouse().getDisplayName());
        assertEquals(eventEntity1.getLegacyVersionLabel(), currentVersion.getVersion());
        assertEquals(eventEntity1.getChronicleId(), currentVersion.getChronicleId());
        assertEquals(eventEntity1.getAntecedentId(), currentVersion.getAntecedentId());
        assertEquals(eventEntity1.getTimestamp(), currentVersion.getEventTs());
        assertEquals(eventEntity1.getIsCurrent(), currentVersion.getIsCurrent());
        assertEquals(eventEntity1.getCreatedDateTime(), currentVersion.getCreatedAt());
        assertEquals(eventEntity1.getCreatedBy().getId(), currentVersion.getCreatedBy());
        assertEquals(eventEntity1.getLastModifiedDateTime(), currentVersion.getLastModifiedAt());
        assertEquals(eventEntity1.getLastModifiedBy().getId(), currentVersion.getLastModifiedBy());
        assertEquals(eventEntity1.isDataAnonymised(), currentVersion.getIsDataAnonymised());

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

        List<AdminGetEventResponseDetails> previousVersions = responseResult.getPreviousVersions();
        assertEquals(3, previousVersions.size());

        assertEquals(eventEntity4.getId(), previousVersions.getFirst().getId());
        assertEquals(eventEntity4.getLegacyObjectId(), previousVersions.getFirst().getDocumentumId());
        assertEquals(eventEntity4.getEventId(), previousVersions.getFirst().getSourceId());
        assertEquals(eventEntity4.getMessageId(), previousVersions.getFirst().getMessageId());
        assertEquals(eventEntity4.getEventText(), previousVersions.getFirst().getText());
        assertEquals(eventEntity4.getEventType().getId(), previousVersions.getFirst().getEventMapping().getId());
        assertEquals(eventEntity4.isLogEntry(), previousVersions.getFirst().getIsLogEntry());
        assertEquals(eventEntity4.getCourtroom().getId(), previousVersions.getFirst().getCourtroom().getId());
        assertEquals(eventEntity4.getCourtroom().getName(), previousVersions.getFirst().getCourtroom().getName());
        assertEquals(eventEntity4.getCourtroom().getCourthouse().getId(), previousVersions.getFirst().getCourthouse().getId());
        assertEquals(eventEntity4.getCourtroom().getCourthouse().getDisplayName(), previousVersions.getFirst().getCourthouse().getDisplayName());
        assertEquals(eventEntity4.getLegacyVersionLabel(), previousVersions.getFirst().getVersion());
        assertEquals(eventEntity4.getChronicleId(), previousVersions.getFirst().getChronicleId());
        assertEquals(eventEntity4.getAntecedentId(), previousVersions.getFirst().getAntecedentId());
        assertEquals(eventEntity4.getTimestamp(), previousVersions.getFirst().getEventTs());
        assertEquals(eventEntity4.getIsCurrent(), previousVersions.getFirst().getIsCurrent());
        assertEquals(eventEntity4.getCreatedDateTime(), previousVersions.getFirst().getCreatedAt());
        assertEquals(eventEntity4.getCreatedBy().getId(), previousVersions.getFirst().getCreatedBy());
        assertEquals(eventEntity4.getLastModifiedDateTime(), previousVersions.getFirst().getLastModifiedAt());
        assertEquals(eventEntity4.getLastModifiedBy().getId(), previousVersions.getFirst().getLastModifiedBy());
        assertEquals(eventEntity4.isDataAnonymised(), previousVersions.getFirst().getIsDataAnonymised());

        assertEquals(eventEntity1.getId(), previousVersions.get(1).getId());
        assertEquals(eventEntity1.getLegacyObjectId(), previousVersions.get(1).getDocumentumId());
        assertEquals(eventEntity1.getEventId(), previousVersions.get(1).getSourceId());
        assertEquals(eventEntity1.getMessageId(), previousVersions.get(1).getMessageId());
        assertEquals(eventEntity1.getEventText(), previousVersions.get(1).getText());
        assertEquals(eventEntity1.getEventType().getId(), previousVersions.get(1).getEventMapping().getId());
        assertEquals(eventEntity1.isLogEntry(), previousVersions.get(1).getIsLogEntry());
        assertEquals(eventEntity1.getCourtroom().getId(), previousVersions.get(1).getCourtroom().getId());
        assertEquals(eventEntity1.getCourtroom().getName(), previousVersions.get(1).getCourtroom().getName());
        assertEquals(eventEntity1.getCourtroom().getCourthouse().getId(), previousVersions.get(1).getCourthouse().getId());
        assertEquals(eventEntity1.getCourtroom().getCourthouse().getDisplayName(), previousVersions.get(1).getCourthouse().getDisplayName());
        assertEquals(eventEntity1.getLegacyVersionLabel(), previousVersions.get(1).getVersion());
        assertEquals(eventEntity1.getChronicleId(), previousVersions.get(1).getChronicleId());
        assertEquals(eventEntity1.getAntecedentId(), previousVersions.get(1).getAntecedentId());
        assertEquals(eventEntity1.getTimestamp(), previousVersions.get(1).getEventTs());
        assertEquals(eventEntity1.getIsCurrent(), previousVersions.get(1).getIsCurrent());
        assertEquals(eventEntity1.getCreatedDateTime(), previousVersions.get(1).getCreatedAt());
        assertEquals(eventEntity1.getCreatedBy().getId(), previousVersions.get(1).getCreatedBy());
        assertEquals(eventEntity1.getLastModifiedDateTime(), previousVersions.get(1).getLastModifiedAt());
        assertEquals(eventEntity1.getLastModifiedBy().getId(), previousVersions.get(1).getLastModifiedBy());
        assertEquals(eventEntity1.isDataAnonymised(), previousVersions.get(1).getIsDataAnonymised());

        assertEquals(eventEntity3.getId(), previousVersions.get(2).getId());
        assertEquals(eventEntity3.getLegacyObjectId(), previousVersions.get(2).getDocumentumId());
        assertEquals(eventEntity3.getEventId(), previousVersions.get(2).getSourceId());
        assertEquals(eventEntity3.getMessageId(), previousVersions.get(2).getMessageId());
        assertEquals(eventEntity3.getEventText(), previousVersions.get(2).getText());
        assertEquals(eventEntity3.getEventType().getId(), previousVersions.get(2).getEventMapping().getId());
        assertEquals(eventEntity3.isLogEntry(), previousVersions.get(2).getIsLogEntry());
        assertEquals(eventEntity3.getCourtroom().getId(), previousVersions.get(2).getCourtroom().getId());
        assertEquals(eventEntity3.getCourtroom().getName(), previousVersions.get(2).getCourtroom().getName());
        assertEquals(eventEntity3.getCourtroom().getCourthouse().getId(), previousVersions.get(2).getCourthouse().getId());
        assertEquals(eventEntity3.getCourtroom().getCourthouse().getDisplayName(), previousVersions.get(2).getCourthouse().getDisplayName());
        assertEquals(eventEntity3.getLegacyVersionLabel(), previousVersions.get(2).getVersion());
        assertEquals(eventEntity3.getChronicleId(), previousVersions.get(2).getChronicleId());
        assertEquals(eventEntity3.getAntecedentId(), previousVersions.get(2).getAntecedentId());
        assertEquals(eventEntity3.getTimestamp(), previousVersions.get(2).getEventTs());
        assertEquals(eventEntity3.getIsCurrent(), previousVersions.get(2).getIsCurrent());
        assertEquals(eventEntity3.getCreatedDateTime(), previousVersions.get(2).getCreatedAt());
        assertEquals(eventEntity3.getCreatedBy().getId(), previousVersions.get(2).getCreatedBy());
        assertEquals(eventEntity3.getLastModifiedDateTime(), previousVersions.get(2).getLastModifiedAt());
        assertEquals(eventEntity3.getLastModifiedBy().getId(), previousVersions.get(2).getLastModifiedBy());
        assertEquals(eventEntity3.isDataAnonymised(), previousVersions.get(2).getIsDataAnonymised());
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
        assertEquals(eventEntity1.getId(), previousVersion.getId());
        assertEquals(eventEntity1.getLegacyObjectId(), previousVersion.getDocumentumId());
        assertEquals(eventEntity1.getEventId(), previousVersion.getSourceId());
        assertEquals(eventEntity1.getMessageId(), previousVersion.getMessageId());
        assertEquals(eventEntity1.getEventText(), previousVersion.getText());
        assertEquals(eventEntity1.getEventType().getId(), previousVersion.getEventMapping().getId());
        assertEquals(eventEntity1.isLogEntry(), previousVersion.getIsLogEntry());
        assertEquals(eventEntity1.getCourtroom().getId(), previousVersion.getCourtroom().getId());
        assertEquals(eventEntity1.getCourtroom().getName(), previousVersion.getCourtroom().getName());
        assertEquals(eventEntity1.getCourtroom().getCourthouse().getId(), previousVersion.getCourthouse().getId());
        assertEquals(eventEntity1.getCourtroom().getCourthouse().getDisplayName(), previousVersion.getCourthouse().getDisplayName());
        assertEquals(eventEntity1.getLegacyVersionLabel(), previousVersion.getVersion());
        assertEquals(eventEntity1.getChronicleId(), previousVersion.getChronicleId());
        assertEquals(eventEntity1.getAntecedentId(), previousVersion.getAntecedentId());
        assertEquals(eventEntity1.getTimestamp(), previousVersion.getEventTs());
        assertEquals(eventEntity1.getIsCurrent(), previousVersion.getIsCurrent());
        assertEquals(eventEntity1.getCreatedDateTime(), previousVersion.getCreatedAt());
        assertEquals(eventEntity1.getCreatedBy().getId(), previousVersion.getCreatedBy());
        assertEquals(eventEntity1.getLastModifiedDateTime(), previousVersion.getLastModifiedAt());
        assertEquals(eventEntity1.getLastModifiedBy().getId(), previousVersion.getLastModifiedBy());
        assertEquals(eventEntity1.isDataAnonymised(), previousVersion.getIsDataAnonymised());
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