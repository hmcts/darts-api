package uk.gov.hmcts.darts.dailylist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.dailylist.api.DailyListsApi;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyList;

import java.time.LocalDate;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Default endpoints per application.
 */
@RestController
public class DailyListController implements DailyListsApi {

    /**
     * POST /dailylist/addDailyList : XHIBIT/CPP send daily case lists to the DAR PC via DARTS. These daily case lists inform the DAR PC which cases are being heard that day within the courthouse for all of its courtrooms.
     * description
     *
     * @param sourceSystem The source system that has sent the message (required)
     * @param dailyList  (required)
     * @return Created (status code 201)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "dailylistAddDailyListPost",
        summary = "XHIBIT/CPP send daily case lists to the DAR PC via DARTS. These daily case lists inform the DAR PC which cases are being heard that day within the courthouse for all of its courtrooms.",
        description = "description",
        tags = { "DailyLists" },
        responses = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/dailylist/addDailyList",
        consumes = { "application/json" }
    )
    @Override
    public ResponseEntity<Void> dailylistAddDailyListPost(
        @NotNull @Parameter(name = "source_system", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "source_system", required = true) String sourceSystem,
        @Parameter(name = "DailyList", description = "", required = true) @Valid @RequestBody DailyList dailyList
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    /**
     * GET /dailylist/getCases : Retrieves the case list for the specified courthouse, courtroom and a hearing date.
     * description
     *
     * @param courtHouseCode The CourtHouseCode to get the daily list for. (required)
     * @param courtRoomNumber The CourtRoomNumber to get the daily list for.&lt;br&gt;This is optional, if not provided, the daily list for all court rooms in the court house will be provided. (optional)
     * @param hearingDate The date to get the daily list for.&lt;br&gt;This is optional, if not provided, the daily list for today will be provided. (optional)
     * @return OK (status code 200)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "dailylistGetCasesGet",
        summary = "Retrieves the case list for the specified courthouse, courtroom and a hearing date",
        description = "description",
        tags = { "DailyLists" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CourtList.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @Override
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/dailylist/getCases",
        produces = { "application/json" }
    )
    public ResponseEntity<CourtList> dailylistGetCasesGet(
        @NotNull @Parameter(name = "court_house_code", description = "The CourtHouseCode to get the daily list for.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_house_code", required = true) Integer courtHouseCode,
        @Parameter(name = "court_room_number", description = "The CourtRoomNumber to get the daily list for.<br>This is optional, if not provided, the daily list for all court rooms in the court house will be provided.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_room_number", required = false) String courtRoomNumber,
        @Parameter(name = "hearing_date", description = "The date to get the daily list for.<br>This is optional, if not provided, the daily list for today will be provided.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "hearing_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hearingDate
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }
}
