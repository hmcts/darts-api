package uk.gov.hmcts.darts.event.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.service.handler.DartsEventNullHandler;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class EventHandlerMapper {

    private final AuthorisationApi authorisationApi;

    public EventHandlerEntity mapFromEventMappingAndMakeActive(EventMapping eventMapping) {
        var eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setType(eventMapping.getType());
        eventHandlerEntity.setSubType(eventMapping.getSubType());
        eventHandlerEntity.setEventName(eventMapping.getName());
        eventHandlerEntity.setHandler(getHandlerMappingOrNullHandler(eventMapping.getHandler()));
        eventHandlerEntity.setReportingRestriction(eventMapping.getHasRestrictions());
        eventHandlerEntity.setActive(true);
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        eventHandlerEntity.setCreatedBy(currentUser);
        eventHandlerEntity.setCreatedDateTime(OffsetDateTime.now());
        return eventHandlerEntity;
    }

    private String getHandlerMappingOrNullHandler(String handler) {
        return StringUtils.isNotBlank(handler) ? handler : DartsEventNullHandler.class.getSimpleName();
    }

    public EventMapping mapToEventMappingResponse(EventHandlerEntity eventHandlerEntity) {
        var eventMapping = new EventMapping();
        eventMapping.setId(eventHandlerEntity.getId());
        eventMapping.setType(eventHandlerEntity.getType());
        eventMapping.setSubType(eventHandlerEntity.getSubType());
        eventMapping.setName(eventHandlerEntity.getEventName());
        eventMapping.setHandler(eventHandlerEntity.getHandler());
        eventMapping.setIsActive(eventHandlerEntity.getActive());
        eventMapping.setHasRestrictions(eventHandlerEntity.isReportingRestriction());
        eventMapping.setCreatedAt(eventHandlerEntity.getCreatedDateTime());

        return eventMapping;
    }
}