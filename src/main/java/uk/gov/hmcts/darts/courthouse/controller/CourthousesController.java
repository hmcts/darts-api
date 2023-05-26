package uk.gov.hmcts.darts.courthouse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.courthouse.api.CourthousesApi;
import uk.gov.hmcts.darts.courthouse.model.CourtHouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourtHouse;

import java.util.List;
import javax.validation.Valid;



@RestController
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CourthousesController implements CourthousesApi {

    /**
     * DELETE /courthouses/{courthouse_id} : Deletes the courthouse entry with the supplied id.
     *
     * @param courthouseId  (required)
     * @return OK (status code 204)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesCourthouseIdDelete",
        summary = "Deletes the court house entery with the supplied id.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "204", description = "OK"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/courthouses/{courthouse_id}"
    )
    @Override
    public ResponseEntity<Void> courthousesCourthouseIdDelete(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * PUT /courthouses/{courthouse_id} : Amends a courthouse record with supplied details.
     *
     * @param courthouseId  (required)
     * @param courtHouse  (required)
     * @return OK (status code 200)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesCourthouseIdPut",
        summary = "Amends a court house record with supplied details.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExtendedCourtHouse.class)))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/courthouses/{courthouse_id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<List<ExtendedCourtHouse>> courthousesCourthouseIdPut(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId,
        @Parameter(name = "CourtHouse", description = "", required = true) @Valid @RequestBody CourtHouse courtHouse
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /courthouses : Gets all courthouse records..
     *
     * @return OK (status code 200)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesGet",
        summary = "Gets all court house records..",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExtendedCourtHouse.class)))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/courthouses",
        produces = { "application/json" }
    )
    @Override
    public ResponseEntity<List<ExtendedCourtHouse>> courthousesGet(

    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * POST /courthouses : Adds a courthouse record with supplied details.
     *
     * @param courtHouse  (required)
     * @return Created (status code 201)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesPost",
        summary = "Adds a courthouse record with supplied details.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/courthouses",
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<Void> courthousesPost(
        @Parameter(name = "CourtHouse", description = "", required = true) @Valid @RequestBody CourtHouse courtHouse
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }
}
