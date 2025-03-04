package uk.gov.hmcts.darts.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetails;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.CourthouseResponseDetails;
import uk.gov.hmcts.darts.event.model.CourtroomResponseDetails;
import uk.gov.hmcts.darts.event.model.EventMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventMapper {

    public AdminGetEventResponseDetails mapToAdminGetEventsResponseForId(EventEntity eventEntity) {
        AdminGetEventResponseDetails adminGetEventResponseDetail = new AdminGetEventResponseDetails();

        adminGetEventResponseDetail.setId(eventEntity.getId());
        adminGetEventResponseDetail.setDocumentumId(eventEntity.getLegacyObjectId());
        adminGetEventResponseDetail.setSourceId(eventEntity.getEventId());
        adminGetEventResponseDetail.setMessageId(eventEntity.getMessageId());
        adminGetEventResponseDetail.setText(eventEntity.getEventText());
        adminGetEventResponseDetail.setIsLogEntry(eventEntity.isLogEntry());

        EventMapping eventMappingDetails = new EventMapping();
        eventMappingDetails.setId(eventEntity.getEventType().getId());
        eventMappingDetails.setName(eventEntity.getEventType().getEventName());
        adminGetEventResponseDetail.setEventMapping(eventMappingDetails);

        CourthouseResponseDetails courthouseResponseDetails = new CourthouseResponseDetails();
        courthouseResponseDetails.setId(eventEntity.getCourtroom().getCourthouse().getId());
        courthouseResponseDetails.setDisplayName(eventEntity.getCourtroom().getCourthouse().getDisplayName());
        adminGetEventResponseDetail.setCourthouse(courthouseResponseDetails);

        CourtroomResponseDetails courtroomResponseDetails = new CourtroomResponseDetails();
        courtroomResponseDetails.setId(eventEntity.getCourtroom().getId());
        courtroomResponseDetails.setName(eventEntity.getCourtroom().getName());
        adminGetEventResponseDetail.setCourtroom(courtroomResponseDetails);

        adminGetEventResponseDetail.setVersion(eventEntity.getLegacyVersionLabel());
        adminGetEventResponseDetail.setChronicleId(eventEntity.getChronicleId());
        adminGetEventResponseDetail.setAntecedentId(eventEntity.getAntecedentId());
        adminGetEventResponseDetail.setEventTs(eventEntity.getTimestamp());
        adminGetEventResponseDetail.isCurrent(eventEntity.getIsCurrent());
        adminGetEventResponseDetail.setCreatedAt(eventEntity.getCreatedDateTime());
        adminGetEventResponseDetail.setCreatedBy(eventEntity.getCreatedBy().getId());
        adminGetEventResponseDetail.setLastModifiedAt(eventEntity.getLastModifiedDateTime());
        adminGetEventResponseDetail.setLastModifiedBy(eventEntity.getLastModifiedBy().getId());
        adminGetEventResponseDetail.setIsDataAnonymised(eventEntity.isDataAnonymised());
        return adminGetEventResponseDetail;
    }

    public AdminGetVersionsByEventIdResponseResult mapToAdminGetEventVersionsResponseForId(List<EventEntity> eventEntities) {
        AdminGetVersionsByEventIdResponseResult adminGetEventsForIdResponse = new AdminGetVersionsByEventIdResponseResult();

        List<AdminGetEventResponseDetails> previousVersions = new ArrayList<>();

        Optional<EventEntity> currentEventEntity = findLatestIsCurrentEvent(eventEntities);

        for (EventEntity eventEntity : eventEntities.stream().sorted(Comparator.comparing(EventEntity::getCreatedDateTime).reversed()).toList()) {
            if (currentEventEntity.isPresent() && eventEntity.equals(currentEventEntity.get())) {
                adminGetEventsForIdResponse.setCurrentVersion(mapToAdminGetEventsResponseForId(eventEntity));
                continue;
            }
            previousVersions.add(mapToAdminGetEventsResponseForId(eventEntity));
        }

        adminGetEventsForIdResponse.setPreviousVersions(previousVersions);

        return adminGetEventsForIdResponse;
    }

    private Optional<EventEntity> findLatestIsCurrentEvent(List<EventEntity> eventEntities) {

        // There could be multiple events with isCurrent = true, so only return the latest one
        return eventEntities.stream()
            .filter(EventEntity::getIsCurrent)
            .max(Comparator.comparing(EventEntity::getCreatedDateTime));
    }
}