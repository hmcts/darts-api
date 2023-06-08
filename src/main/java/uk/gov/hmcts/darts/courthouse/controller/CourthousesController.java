package uk.gov.hmcts.darts.courthouse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.courthouse.api.CourthousesApi;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import java.util.List;
import javax.validation.Valid;

@RestController
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CourthousesController implements CourthousesApi {

    @Autowired
    CourthouseService courthouseService;

    @Autowired
    CourthouseToCourthouseEntityMapper mapper;

    /**
     * DELETE /courthouses/{courthouse_id} : Deletes the courthouse entry with the supplied id.
     *
     * @param courthouseId  (required)
     * @return OK (status code 204)
     *         or A required parameter is missing or an invalid datatype or value was provided for property. (status code 400)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesCourthouseIdDelete",
        summary = "Deletes the courthouse entry with the supplied id.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "204", description = "OK"),
            @ApiResponse(responseCode = "400", description = "A required parameter is missing or an invalid datatype or value was provided for property."),
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
        courthouseService.deleteCourthouseById(courthouseId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }

    /**
     * GET /courthouses/{courthouse_id} : Get a courthouse record with specified id.
     *
     * @param courthouseId  (required)
     * @return OK (status code 200)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesCourthouseIdGet",
        summary = "Get a courthouse record with specified id.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ExtendedCourthouse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/courthouses/{courthouse_id}",
        produces = { "application/json" }
    )
    @Override
    public ResponseEntity<ExtendedCourthouse> courthousesCourthouseIdGet(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId
    ) {
        try {
            uk.gov.hmcts.darts.common.entity.Courthouse courtHouseEntity = courthouseService.getCourtHouseById(
                courthouseId);
            ExtendedCourthouse responseEntity = mapper.mapFromEntityToExtendedCourthouse(courtHouseEntity);
            return new ResponseEntity<>(responseEntity, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }


    }

    /**
     * PUT /courthouses/{courthouse_id} : Amends a courthouse record with supplied details.
     *
     * @param courthouseId  (required)
     * @param courthouse  (required)
     * @return No Content (status code 204)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesCourthouseIdPut",
        summary = "Amends a courthouse record with supplied details.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/courthouses/{courthouse_id}",
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<Void> courthousesCourthouseIdPut(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId,
        @Parameter(name = "Courthouse", description = "", required = true) @Valid @RequestBody Courthouse courthouse
    ) {
        courthouseService.amendCourthouseById(courthouse, courthouseId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }

    /**
     * GET /courthouses : Gets all courthouse records..
     *
     * @return OK (status code 200)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesGet",
        summary = "Gets all courthouse records..",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExtendedCourthouse.class)))
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
    public ResponseEntity<List<ExtendedCourthouse>> courthousesGet(

    ) {
        List<uk.gov.hmcts.darts.common.entity.Courthouse> courtHouseEntities = courthouseService.getAllCourthouses();
        List<ExtendedCourthouse> responseEntities = mapper.mapFromListEntityToListExtendedCourthouse(courtHouseEntities);
        return new ResponseEntity<>(responseEntities, HttpStatus.OK);
    }

    /**
     * POST /courthouses : Adds a courthouse record with supplied details.
     *
     * @param courthouse  (required)
     * @return Created (status code 201)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "courthousesPost",
        summary = "Adds a courthouse record with supplied details.",
        tags = { "Courthouses" },
        responses = {
            @ApiResponse(responseCode = "201", description = "Created", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ExtendedCourthouse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/courthouses",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<ExtendedCourthouse> courthousesPost(
        @Parameter(name = "Courthouse", description = "", required = true) @Valid @RequestBody Courthouse courthouse
    ) {
        uk.gov.hmcts.darts.common.entity.Courthouse addedCourtHouse = courthouseService.addCourtHouse(courthouse);
        ExtendedCourthouse extendedCourthouse = mapper.mapFromEntityToExtendedCourthouse(addedCourtHouse);
        return new ResponseEntity<>(extendedCourthouse, HttpStatus.CREATED);
    }
}
