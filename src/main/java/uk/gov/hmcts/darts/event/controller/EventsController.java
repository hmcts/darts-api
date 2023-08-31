package uk.gov.hmcts.darts.event.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.event.component.DartsEventMapper;
import uk.gov.hmcts.darts.event.service.CourtLogsService;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.events.api.EventApi;
import uk.gov.hmcts.darts.events.model.CourtLog;
import uk.gov.hmcts.darts.events.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.events.model.DartsEvent;
import uk.gov.hmcts.darts.events.model.EventsResponse;

import java.time.OffsetDateTime;
import java.util.List;
import javax.validation.Valid;

@Slf4j
@RequiredArgsConstructor
@RestController
@SuppressWarnings({"checkstyle.LineLengthCheck"})
public class EventsController implements EventApi {

    private final CourtLogsService courtLogsService;

    private final EventDispatcher eventDispatcher;
    private final DartsEventMapper dartsEventMapper;

    @Operation(
        operationId = "eventsPost",
        summary = "An Endpoint which allows users to request and submit events.",
        description = "Event data received from XHIBIT and CPP through a custom web service to add context to the audio recordings it stores.",
        tags = {"Event"},
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = EventsResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/events",
        produces = {"application/json"},
        consumes = {"application/json"}
    )
    @Override
    public ResponseEntity<EventsResponse> eventsPost(
        @Parameter(name = "DartsEvent") @Valid @RequestBody DartsEvent dartsEvent
    ) {
        eventDispatcher.receive(dartsEvent);

        var addDocumentResponse = new EventsResponse();

        var status = HttpStatus.CREATED;
        addDocumentResponse.setCode(String.valueOf(status.value()));
        addDocumentResponse.setMessage(status.name());

        return new ResponseEntity<>(addDocumentResponse, status);
    }

    @Override
    public ResponseEntity<EventsResponse> courtlogsPost(CourtLogsPostRequestBody courtLogsPostRequestBody) {
        DartsEvent dartsEvent = dartsEventMapper.toDartsEvent(courtLogsPostRequestBody);

        return eventsPost(dartsEvent);
    }

    @Override
    public ResponseEntity<List<CourtLog>> courtlogsGet(
        @Parameter(name = "courthouse", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = true) String courthouse,
        @Parameter(name = "case_number", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "case_number", required = true) String caseNumber,
        @Parameter(name = "start_date_time", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "start_date_time", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
        @Parameter(name = "end_date_time", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "end_date_time", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime
    ) {

        List<CourtLog> courtLogs = courtLogsService.getCourtLogs(courthouse, caseNumber, startDateTime, endDateTime);

        return new ResponseEntity<>(courtLogs, HttpStatus.OK);

    }

}
