package uk.gov.hmcts.darts.events.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.event.api.EventApi;
import uk.gov.hmcts.darts.event.model.AddDocumentResponse;

import java.math.BigDecimal;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;



@Slf4j
@RestController
public class EventsController implements EventApi {

    @Operation(
        operationId = "eventAddDocumentPost",
        summary = "SORT OUT LATER.",
        description = "description",
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
        value = "/event/addDocument",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<AddDocumentResponse> eventAddDocumentPost(
        @NotNull @Parameter(name = "message_id", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "message_id", required = true) BigDecimal messageId,
        @NotNull @Parameter(name = "type", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "type", required = true) String type,
        @NotNull @Parameter(name = "sub_type", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "sub_type", required = true) String subType,
        @Parameter(name = "body", description = "", required = true) @Valid @RequestBody String body
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
