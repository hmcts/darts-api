package uk.gov.hmcts.darts.dailylist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.api.DailyListsApi;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyList;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Default endpoints per application.
 */
@SuppressWarnings({"checkstyle.LineLengthCheck"})
@RestController
public class DailyListController implements DailyListsApi {

    @Autowired
    CourthouseRepository courthouseRepository;
    @Autowired
    private DailyListService dailyListService;
    @Autowired
    private DailyListProcessor processor;

    @Operation(
        operationId = "dailylistsPost",
        summary = "XHIBIT/CPP send daily case lists to the DAR PC via DARTS. These daily case lists inform the DAR PC which cases are being heard that day within the courthouse for all of its courtrooms.",
        description = "description",
        tags = {"DailyLists"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/dailylists",
        consumes = {"application/json"}
    )
    @Override
    public ResponseEntity<Void> dailylistsPost(
        @NotNull @Parameter(name = "source_system", description = "The source system that has sent the message", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "source_system", required = true) String sourceSystem,
        @Parameter(name = "DailyList", description = "", required = true) @Valid @RequestBody DailyList dailyList
    ) {
        DailyListPostRequest postRequest = new DailyListPostRequest();
        postRequest.setSourceSystem(sourceSystem);
        postRequest.setDailyList(dailyList);

        dailyListService.processIncomingDailyList(postRequest);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @Override
    @Operation(
        operationId = "dailylistsGetCasesGet",
        summary = "Retrieves the case list for the specified courthouse, courtroom and a hearing date.",
        description = "description",
        tags = {"DailyLists"},
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = CourtList.class))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/dailylists/getCases",
        produces = {"application/json"}
    )
    public ResponseEntity<CourtList> dailylistsGetCasesGet(
        @NotNull @Parameter(name = "court_house_code", description = "The CourtHouseCode to get the daily list for.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_house_code", required = true) Integer courtHouseCode,
        @Parameter(name = "court_room_number", description = "The CourtRoomNumber to get the daily list for.<br>This is optional, if not provided, the daily list for all court rooms in the court house will be provided.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "court_room_number", required = false) String courtRoomNumber,
        @Parameter(name = "hearing_date", description = "The date to get the daily list for.<br>This is optional, if not provided, the daily list for today will be provided.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "hearing_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hearingDate
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }


    public ResponseEntity<Void> dailylistsHousekeepingPost() {
        dailyListService.runHouseKeepingNow();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> dailylistsRunPost(Integer courthouseId) {

        if (courthouseId == null) {
            CompletableFuture.runAsync(() -> processor.processAllDailyLists(LocalDate.now()));
        } else {
            Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findById(
                courthouseId);
            foundCourthouse.ifPresentOrElse(
                courthouse -> CompletableFuture.runAsync(() -> processor.processAllDailyListForCourthouse(courthouse)),
                () -> {
                    throw new DartsApiException(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST);
                }
            );

        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);

    }

}
