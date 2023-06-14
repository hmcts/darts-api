package uk.gov.hmcts.darts.event.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.event.api.EventApi;
import uk.gov.hmcts.darts.event.model.AddDocumentResponse;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import javax.validation.Valid;

@Slf4j
@RestController
public class EventsController implements EventApi {

    @Operation(
        operationId = "eventsPost",
        summary = "An Endpoint which allows users to request and submit events.",
        description = "Event data received from XHIBIT and CPP through a custom web service to add context to the audio recordings it stores.",
        tags = { "Event" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = AddDocumentResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/events",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<AddDocumentResponse> eventsPost(
        @Parameter(name = "DartsEvent", description = "") @Valid @RequestBody(required = false) DartsEvent dartsEvent
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
