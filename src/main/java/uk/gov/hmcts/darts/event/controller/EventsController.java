package uk.gov.hmcts.darts.event.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.component.DartsEventMapper;
import uk.gov.hmcts.darts.event.http.api.EventApi;
import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetails;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.AdminObfuscateEveByIdsRequest;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResult;
import uk.gov.hmcts.darts.event.model.CourtLog;
import uk.gov.hmcts.darts.event.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.event.model.EventsResponse;
import uk.gov.hmcts.darts.event.service.CourtLogsService;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.event.service.EventMappingService;
import uk.gov.hmcts.darts.event.service.EventSearchService;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.event.service.handler.EventHandlerEnumerator;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;
import java.util.List;
import javax.validation.Valid;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
@SuppressWarnings({"checkstyle.LineLengthCheck", "PMD.TooManyMethods"})
public class EventsController implements EventApi {

    private final CourtLogsService courtLogsService;
    private final EventDispatcher eventDispatcher;
    private final DartsEventMapper dartsEventMapper;
    private final EventMappingService eventMappingService;
    private final EventHandlerEnumerator eventHandlers;
    private final EventSearchService eventSearchService;
    private final EventService eventService;
    private final DataAnonymisationService dataAnonymisationService;
    private final UserIdentity userIdentity;

    @Value("${darts.event-obfuscation.enabled}")
    private final boolean eventObfuscationEnabled;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<EventsResponse> eventsPost(
        @Parameter(name = "DartsEvent") @Valid @RequestBody DartsEvent dartsEvent
    ) {
        DataUtil.preProcess(dartsEvent);
        eventDispatcher.receive(dartsEvent);

        var addDocumentResponse = new EventsResponse();

        var status = HttpStatus.CREATED;
        addDocumentResponse.setCode(String.valueOf(status.value()));
        addDocumentResponse.setMessage(status.name());

        return new ResponseEntity<>(addDocumentResponse, status);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {MID_TIER})
    public ResponseEntity<EventsResponse> courtlogsPost(CourtLogsPostRequestBody courtLogsPostRequestBody) {
        DartsEvent dartsEvent = dartsEventMapper.toDartsEvent(courtLogsPostRequestBody);

        return eventsPost(dartsEvent);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT, CPP})
    public ResponseEntity<List<CourtLog>> courtlogsGet(
        @Parameter(name = "courthouse", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = true) String courthouse,
        @Parameter(name = "case_number", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "case_number", required = true)
        String caseNumber,
        @Parameter(name = "start_date_time", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "start_date_time", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
        @Parameter(name = "end_date_time", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "end_date_time", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime
    ) {

        List<CourtLog> courtLogs = courtLogsService.getCourtLogs(courthouse, caseNumber, startDateTime, endDateTime);

        return new ResponseEntity<>(courtLogs, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<EventMapping> adminPostEventMapping(EventMapping eventMapping, Boolean isRevision) {

        return new ResponseEntity<>(eventMappingService.postEventMapping(eventMapping, isRevision), HttpStatus.CREATED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<List<String>> adminGetEventHandlers() {
        return new ResponseEntity<>(eventHandlers.obtainHandlers(), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<EventMapping>> adminGetEventMappings() {

        List<EventMapping> eventMappings = eventMappingService.getEventMappings();

        return new ResponseEntity<>(eventMappings, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<EventMapping> adminGetEventMappingById(Integer eventHandlerId) {

        EventMapping eventMapping = eventMappingService.getEventMappingById(eventHandlerId);

        return new ResponseEntity<>(eventMapping, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<Void> adminDeleteEventMappings(Integer eventHandlerId) {
        eventMappingService.deleteEventMapping(eventHandlerId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<AdminSearchEventResponseResult>> adminSearchEvents(AdminEventSearch adminEventSearch) {
        var adminSearchEventResponse = eventSearchService.searchForEvents(adminEventSearch);
        return new ResponseEntity<>(adminSearchEventResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<AdminGetEventResponseDetails> adminGetEventById(Integer eventId) {
        return new ResponseEntity<>(eventService.adminGetEventById(eventId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<AdminGetVersionsByEventIdResponseResult> adminGetVersionsByEventId(Integer eventId) {
        return new ResponseEntity<>(eventService.adminGetVersionsByEventId(eventId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<Void> adminObfuscateEveByIds(AdminObfuscateEveByIdsRequest adminObfuscateEveByIdsRequest) {
        if (!eventObfuscationEnabled) {
            throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Event obfuscation is not enabled");
        }
        this.dataAnonymisationService.anonymiseEventByIds(userIdentity.getUserAccount(), adminObfuscateEveByIdsRequest.getEveIds(), true);
        return ResponseEntity.ok().build();
    }
}