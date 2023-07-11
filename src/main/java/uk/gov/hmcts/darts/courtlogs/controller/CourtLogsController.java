package uk.gov.hmcts.darts.courtlogs.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.courtlogs.api.CourtLogsApi;
import uk.gov.hmcts.darts.courtlogs.model.CourtLogs;

import java.time.OffsetDateTime;
import javax.validation.Valid;

@RestController
public class CourtLogsController implements CourtLogsApi {

    @Override
    public ResponseEntity<CourtLogs> courtlogsGet(
        @Parameter(name = "courthouse", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = true) String courthouse,
        @Parameter(name = "caseNumber", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "caseNumber", required = true) String caseNumber,
        @Parameter(name = "startDateTime", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "startDateTime", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
        @Parameter(name = "endDateTime", description = "", in = ParameterIn.QUERY) @Valid @RequestParam(value = "endDateTime", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
