package uk.gov.hmcts.darts.hearings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.hearings.api.HearingsApi;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.model.Problem;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

@RestController
@RequiredArgsConstructor
public class HearingsController implements HearingsApi {

    private final HearingsService hearingsService;

    @Override
    @Operation(
        operationId = "hearingsHearingIdGet",
        summary = "Allows the retrieval of a hearing by ID.",
        tags = {"Hearings"},
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = GetHearingResponse.class)),
                @Content(mediaType = "application/json+problem", schema = @Schema(implementation = GetHearingResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class)),
                @Content(mediaType = "application/json+problem", schema = @Schema(implementation = Problem.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/hearings/{hearing_id}",
        produces = {"application/json", "application/json+problem"}
    )
    public ResponseEntity<GetHearingResponse> hearingsHearingIdGet(
        @Parameter(name = "hearing_id", description = "Internal hea_id of the case.", required = true, in = ParameterIn.PATH)
        @PathVariable("hearing_id") Integer hearingId
    ) {
        return new ResponseEntity<>(hearingsService.getHearings(hearingId), HttpStatus.OK);

    }
}
