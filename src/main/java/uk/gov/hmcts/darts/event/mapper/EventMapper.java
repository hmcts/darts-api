package uk.gov.hmcts.darts.event.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventById200Response;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetails;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetailsCasesCasesInner;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetailsHearingsHearingsInner;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.CourthouseResponseDetails;
import uk.gov.hmcts.darts.event.model.CourtroomResponseDetails;
import uk.gov.hmcts.darts.event.model.EventMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class EventMapper {

    public AdminGetEventResponseDetails mapToAdminGetEventResponseDetails(EventEntity eventEntity) {
        return mapToAdminGetEventsResponseForId(eventEntity, new AdminGetEventResponseDetails());
    }

    public AdminGetEventById200Response mapToAdminGetEventById200Response(EventEntity eventEntity) {
        AdminGetEventById200Response response = new AdminGetEventById200Response();
        mapToAdminGetEventsResponseForId(eventEntity, response);

        response.setCases(mapAdminGetEventResponseDetailsCasesCases(eventEntity.getLinkedCases()));
        response.setHearings(mapAdminGetEventResponseDetailsHearings(eventEntity.getHearingEntities()));
        return response;
    }

    private <T extends AdminGetEventResponseDetails> T mapToAdminGetEventsResponseForId(EventEntity eventEntity, T adminGetEventResponseDetail) {
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
        adminGetEventResponseDetail.setCreatedBy(eventEntity.getCreatedById());
        adminGetEventResponseDetail.setLastModifiedAt(eventEntity.getLastModifiedDateTime());
        adminGetEventResponseDetail.setLastModifiedBy(eventEntity.getLastModifiedById());
        adminGetEventResponseDetail.setIsDataAnonymised(eventEntity.isDataAnonymised());
        adminGetEventResponseDetail.setEventStatus(eventEntity.getEventStatus());
        return adminGetEventResponseDetail;
    }


    List<AdminGetEventResponseDetailsHearingsHearingsInner> mapAdminGetEventResponseDetailsHearings(Collection<HearingEntity> hearingEntities) {
        if (CollectionUtils.isEmpty(hearingEntities)) {
            return new ArrayList<>();
        }
        return hearingEntities.stream()
            .sorted(Comparator.comparing(HearingEntity::getId))
            .map(this::mapAdminGetEventResponseDetailsHearing)
            .toList();
    }

    AdminGetEventResponseDetailsHearingsHearingsInner mapAdminGetEventResponseDetailsHearing(HearingEntity hearingEntity) {
        if (hearingEntity == null) {
            return null;
        }
        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner =
            new AdminGetEventResponseDetailsHearingsHearingsInner();
        adminGetEventResponseDetailsHearingsHearingsInner.setId(hearingEntity.getId());
        adminGetEventResponseDetailsHearingsHearingsInner.setCaseId(hearingEntity.getCourtCase().getId());
        adminGetEventResponseDetailsHearingsHearingsInner.setCaseNumber(hearingEntity.getCourtCase().getCaseNumber());
        adminGetEventResponseDetailsHearingsHearingsInner.setHearingDate(hearingEntity.getHearingDate());
        adminGetEventResponseDetailsHearingsHearingsInner.setCourtroom(mapCourtRoom(hearingEntity.getCourtroom()));
        adminGetEventResponseDetailsHearingsHearingsInner.setCourthouse(
            mapCourtHouse(Optional.ofNullable(hearingEntity.getCourtroom())
                              .map(CourtroomEntity::getCourthouse)
                              .orElse(null)));
        return adminGetEventResponseDetailsHearingsHearingsInner;
    }

    List<AdminGetEventResponseDetailsCasesCasesInner> mapAdminGetEventResponseDetailsCasesCases(List<CourtCaseEntity> cases) {
        if (CollectionUtils.isEmpty(cases)) {
            return new ArrayList<>();
        }
        return cases.stream()
            .map(this::mapAdminGetEventResponseDetailsCasesCase)
            .toList();
    }

    AdminGetEventResponseDetailsCasesCasesInner mapAdminGetEventResponseDetailsCasesCase(CourtCaseEntity caseEntity) {
        if (caseEntity == null) {
            return null;
        }
        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner = new AdminGetEventResponseDetailsCasesCasesInner();

        adminGetEventResponseDetailsCasesCasesInner.setId(caseEntity.getId());
        adminGetEventResponseDetailsCasesCasesInner.setCaseNumber(caseEntity.getCaseNumber());
        adminGetEventResponseDetailsCasesCasesInner.setCourthouse(mapCourtHouse(caseEntity.getCourthouse()));
        return adminGetEventResponseDetailsCasesCasesInner;
    }

    CourthouseResponseDetails mapCourtHouse(CourthouseEntity courthouse) {
        if (courthouse == null) {
            return null;
        }
        CourthouseResponseDetails courthouseResponseDetails = new CourthouseResponseDetails();
        courthouseResponseDetails.setId(courthouse.getId());
        courthouseResponseDetails.setDisplayName(courthouse.getDisplayName());
        return courthouseResponseDetails;
    }

    CourtroomResponseDetails mapCourtRoom(CourtroomEntity courtroom) {
        if (courtroom == null) {
            return null;
        }
        CourtroomResponseDetails courthouseResponseDetails = new CourtroomResponseDetails();
        courthouseResponseDetails.setId(courtroom.getId());
        courthouseResponseDetails.setName(courtroom.getName());
        return courthouseResponseDetails;
    }

    public AdminGetVersionsByEventIdResponseResult mapToAdminGetEventVersionsResponseForId(List<EventEntity> eventEntities) {
        AdminGetVersionsByEventIdResponseResult adminGetEventsForIdResponse = new AdminGetVersionsByEventIdResponseResult();

        List<AdminGetEventResponseDetails> previousVersions = new ArrayList<>();

        Optional<EventEntity> currentEventEntity = findLatestIsCurrentEvent(eventEntities);

        for (EventEntity eventEntity : eventEntities.stream().sorted(Comparator.comparing(EventEntity::getCreatedDateTime).reversed()).toList()) {
            if (currentEventEntity.isPresent() && eventEntity.equals(currentEventEntity.get())) {
                adminGetEventsForIdResponse.setCurrentVersion(mapToAdminGetEventResponseDetails(eventEntity));
                continue;
            }
            previousVersions.add(mapToAdminGetEventResponseDetails(eventEntity));
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