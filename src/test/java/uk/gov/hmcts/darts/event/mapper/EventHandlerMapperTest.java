package uk.gov.hmcts.darts.event.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.EventMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventHandlerMapperTest {

    private static final UserAccountEntity USER_ACCOUNT_ENTITY = new UserAccountEntity();

    @Mock
    private AuthorisationApi authorisationApi;

    private EventHandlerMapper eventHandlerMapper;

    private void setUp() {
        when(authorisationApi.getCurrentUser()).thenReturn(USER_ACCOUNT_ENTITY);

        eventHandlerMapper = new EventHandlerMapper(authorisationApi);
    }

    @Test
    void mapsEventMapperToEventHandlerEntityCorrectly() {
        setUp();
        var eventMapping = someEventMapping();

        assertThat(eventHandlerMapper.mapFromEventMappingAndMakeActive(eventMapping))
            .hasFieldOrPropertyWithValue("type", eventMapping.getType())
            .hasFieldOrPropertyWithValue("subType", eventMapping.getSubType())
            .hasFieldOrPropertyWithValue("eventName", eventMapping.getName())
            .hasFieldOrPropertyWithValue("handler", eventMapping.getHandler())
            .hasFieldOrPropertyWithValue("isReportingRestriction", eventMapping.getHasRestrictions())
            .hasFieldOrPropertyWithValue("active", true)
            .hasFieldOrPropertyWithValue("createdBy", USER_ACCOUNT_ENTITY);
    }

    @Test
    void mapsEventMapperToEventHandlerEntityCorrectlyWhenHandlerIsEmpty() {
        setUp();
        var eventMapping = someEventMapping();
        eventMapping.setHandler(null);

        assertThat(eventHandlerMapper.mapFromEventMappingAndMakeActive(eventMapping))
            .hasFieldOrPropertyWithValue("type", eventMapping.getType())
            .hasFieldOrPropertyWithValue("subType", eventMapping.getSubType())
            .hasFieldOrPropertyWithValue("eventName", eventMapping.getName())
            .hasFieldOrPropertyWithValue("handler", "DartsEventNullHandler")
            .hasFieldOrPropertyWithValue("isReportingRestriction", eventMapping.getHasRestrictions())
            .hasFieldOrPropertyWithValue("active", true)
            .hasFieldOrPropertyWithValue("createdBy", USER_ACCOUNT_ENTITY);
    }

    @Test
    void mapsEventHandlerEntityToEventMappingCorrectly() {
        eventHandlerMapper = new EventHandlerMapper(authorisationApi);

        var eventHandlerEntity = someEventHandlerEntity();

        assertThat(eventHandlerMapper.mapToEventMappingResponse(eventHandlerEntity))
            .hasFieldOrPropertyWithValue("id", eventHandlerEntity.getId())
            .hasFieldOrPropertyWithValue("type", eventHandlerEntity.getType())
            .hasFieldOrPropertyWithValue("subType", eventHandlerEntity.getSubType())
            .hasFieldOrPropertyWithValue("name", eventHandlerEntity.getEventName())
            .hasFieldOrPropertyWithValue("handler", eventHandlerEntity.getHandler())
            .hasFieldOrPropertyWithValue("hasRestrictions", eventHandlerEntity.getIsReportingRestriction())
            .hasFieldOrPropertyWithValue("isActive", true)
            .hasFieldOrPropertyWithValue("hasEvents", null);
    }

    private EventMapping someEventMapping() {
        EventMapping mapping = new EventMapping();
        mapping.setType("12345");
        mapping.setSubType("9876");
        mapping.setName("My Event");
        mapping.setHandler("Standard Handler");
        mapping.setHasRestrictions(false);
        return mapping;
    }

    private EventHandlerEntity someEventHandlerEntity() {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setId(1);
        eventHandlerEntity.setType("12345");
        eventHandlerEntity.setSubType("9876");
        eventHandlerEntity.setEventName("My Event");
        eventHandlerEntity.setHandler("Standard Handler");
        eventHandlerEntity.setIsReportingRestriction(false);
        eventHandlerEntity.setActive(true);
        return eventHandlerEntity;
    }
}
