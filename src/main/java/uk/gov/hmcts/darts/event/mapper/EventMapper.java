package uk.gov.hmcts.darts.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;
import uk.gov.hmcts.darts.event.model.CourthouseResponseDetails;
import uk.gov.hmcts.darts.event.model.CourtroomResponseDetails;
import uk.gov.hmcts.darts.event.model.EventMappingDetails;

@Component
@RequiredArgsConstructor
public class EventMapper {

    public AdminGetEventForIdResponseResult mapToAdminGetEventsResponseForId(EventEntity eventEntity) {
        AdminGetEventForIdResponseResult adminGetEventsForIdResponse = new AdminGetEventForIdResponseResult();

        adminGetEventsForIdResponse.setId(eventEntity.getId());
        adminGetEventsForIdResponse.setDocumentumId(eventEntity.getLegacyObjectId());
        adminGetEventsForIdResponse.setSourceId(eventEntity.getEventId());
        adminGetEventsForIdResponse.setMessageId(eventEntity.getMessageId());
        adminGetEventsForIdResponse.setText(eventEntity.getEventText());
        adminGetEventsForIdResponse.setIsLogEntry(eventEntity.isLogEntry());

        EventMappingDetails eventMappingDetails = new EventMappingDetails();
        eventMappingDetails.setId(eventEntity.getEventType().getId());
        adminGetEventsForIdResponse.setEventMapping(eventMappingDetails);

        CourthouseResponseDetails courthouseResponseDetails = new CourthouseResponseDetails();
        courthouseResponseDetails.setId(eventEntity.getCourtroom().getCourthouse().getId());
        courthouseResponseDetails.setDisplayName(eventEntity.getCourtroom().getCourthouse().getDisplayName());
        adminGetEventsForIdResponse.setCourthouse(courthouseResponseDetails);

        CourtroomResponseDetails courtroomResponseDetails = new CourtroomResponseDetails();
        courtroomResponseDetails.setId(eventEntity.getCourtroom().getId());
        courtroomResponseDetails.setName(eventEntity.getCourtroom().getName());
        adminGetEventsForIdResponse.setCourtroom(courtroomResponseDetails);

        adminGetEventsForIdResponse.setVersion(eventEntity.getLegacyVersionLabel());
        adminGetEventsForIdResponse.setChronicleId(eventEntity.getChronicleId());
        adminGetEventsForIdResponse.setAntecedentId(eventEntity.getAntecedentId());
        adminGetEventsForIdResponse.setEventTs(eventEntity.getTimestamp());
        adminGetEventsForIdResponse.isCurrent(eventEntity.getIsCurrent());
        adminGetEventsForIdResponse.setCreatedAt(eventEntity.getCreatedDateTime());
        adminGetEventsForIdResponse.setCreatedBy(eventEntity.getCreatedBy().getId());
        adminGetEventsForIdResponse.setLastModifiedAt(eventEntity.getLastModifiedDateTime());
        adminGetEventsForIdResponse.setLastModifiedBy(eventEntity.getLastModifiedBy().getId());
        adminGetEventsForIdResponse.setIsDataAnonymised(eventEntity.isDataAnonymised());
        return adminGetEventsForIdResponse;
    }
}