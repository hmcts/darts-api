package uk.gov.hmcts.darts.cases.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.cases.api.CasesApi;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.service.CaseService;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
public class CaseController implements CasesApi {

    private final CaseService caseService;

    @Override
    /**
     * GET /cases : Allows the retrieval of all cases for a given courthouse, courtroom and date
     * Retrieves all cases for a given courthouse, courtroom and date
     *
     * @param courthouse The name of the courthouse (required)
     * @param courtroom The name of the courtroom (required)
     * @param date The date to get the cases for. Normally today (required)
     * @return OK (status code 200)
     *         or Internal Server Error (status code 500)
     */
    @Operation(
        operationId = "casesGet",
        summary = "Allows the retrieval of all cases for a given courthouse, courtroom and date",
        description = "Retrieves all cases for a given courthouse, courtroom and date",
        tags = {"Cases"},
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ScheduledCase.class)))
            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/cases",
        produces = {"application/json"}
    )
    public ResponseEntity<List<ScheduledCase>> casesGet(
        @NotNull @Parameter(name = "courthouse", description = "The name of the courthouse", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = true) String courthouse,
        @NotNull @Parameter(name = "courtroom", description = "The name of the courtroom", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "courtroom", required = true) String courtroom,
        @NotNull @Parameter(name = "date", description = "The date to get the cases for. Normally today", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(courthouse.toUpperCase(Locale.ROOT));
        request.setCourtroom(courtroom.toUpperCase(Locale.ROOT));
        request.setDate(date);


        return new ResponseEntity<>(caseService.getCases(request), HttpStatus.OK);

    }
}
