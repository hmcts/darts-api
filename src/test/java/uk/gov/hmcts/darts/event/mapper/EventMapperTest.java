package uk.gov.hmcts.darts.event.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;

import java.time.OffsetDateTime;

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
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId(1);
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
        eventEntity.setCreatedDateTime(OffsetDateTime.now());
        eventEntity.setCreatedBy(uaCreatedEntity);

        UserAccountEntity uaLastModifiedEntity = new UserAccountEntity();
        eventEntity.setLastModifiedDateTime(OffsetDateTime.now());
        eventEntity.setLastModifiedBy(uaLastModifiedEntity);
        eventEntity.setTimestamp(OffsetDateTime.now());
        eventEntity.setIsCurrent(true);
        // Given
        AdminGetEventForIdResponseResult responseResult = eventMapper.mapToAdminGetEventsResponseForId(eventEntity);

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
        Assertions.assertEquals(eventEntity.getTimestamp(), responseResult.getEventTs());
        Assertions.assertEquals(eventEntity.getIsCurrent(), responseResult.getIsCurrent());
        Assertions.assertEquals(eventEntity.getCreatedDateTime(), responseResult.getCreatedAt());
        Assertions.assertEquals(eventEntity.getCreatedBy().getId(), responseResult.getCreatedBy());
        Assertions.assertEquals(eventEntity.getLastModifiedDateTime(), responseResult.getLastModifiedAt());
        Assertions.assertEquals(eventEntity.getLastModifiedBy().getId(), responseResult.getLastModifiedBy());
        Assertions.assertEquals(eventEntity.isDataAnonymised(), responseResult.getIsDataAnonymised());
    }
}