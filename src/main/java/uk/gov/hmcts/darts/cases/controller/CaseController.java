package uk.gov.hmcts.darts.cases.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.api.CasesApi;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.EventResponse;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.RequestValidator;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static uk.gov.hmcts.darts.cases.exception.CaseApiError.CASE_NOT_FOUND;

@RestController
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI", "checkstyle.LineLengthCheck",
    "PMD.ExcessiveImports"})
@Slf4j
public class CaseController implements CasesApi {

    private final CaseService caseService;
    private final CaseRepository caseRepository;
    private final AuthorisationApi authorisationApi;

    @Override
    public ResponseEntity<List<ScheduledCase>> casesGet(
        @NotNull @Parameter(name = "courthouse", description = "The name of the courthouse", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = true) String courthouse,
        @NotNull @Parameter(name = "courtroom", description = "The name of the courtroom", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "courtroom", required = true) String courtroom,
        @NotNull @Parameter(name = "date", description = "The date to get the cases for. Normally today", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(courthouse.toUpperCase(Locale.ROOT));
        request.setCourtroom(courtroom.toUpperCase(Locale.ROOT));
        request.setDate(date);

        return new ResponseEntity<>(caseService.getHearings(request), HttpStatus.OK);

    }

    @Override
    public ResponseEntity<PostCaseResponse> casesPost(
        @Parameter(name = "AddCaseRequest", description = "", required = true) @RequestBody AddCaseRequest addCaseRequest) {
        return new ResponseEntity<>(caseService.addCaseOrUpdate(addCaseRequest), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<AdvancedSearchResult>> casesSearchGet(
        @Size(max = 20) @Parameter(name = "case_number", description = "Full or partial Case Number", in = ParameterIn.QUERY) @Valid @RequestParam(value = "case_number", required = false) String caseNumber,
        @Size(max = 30) @Parameter(name = "courthouse", description = "Full or partial Courthouse name", in = ParameterIn.QUERY) @Valid @RequestParam(value = "courthouse", required = false) String courthouse,
        @Size(max = 30) @Parameter(name = "courtroom", description = "Full or partial Courtroom name", in = ParameterIn.QUERY) @Valid @RequestParam(value = "courtroom", required = false) String courtroom,
        @Size(max = 30) @Parameter(name = "judge_name", description = "Full or partial Judge name", in = ParameterIn.QUERY) @Valid @RequestParam(value = "judge_name", required = false) String judgeName,
        @Size(max = 30) @Parameter(name = "defendant_name", description = "Full or partial Defendant name", in = ParameterIn.QUERY) @Valid @RequestParam(value = "defendant_name", required = false) String defendantName,
        @Parameter(name = "date_from", description = "DateFrom to search for the hearings in.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @Parameter(name = "date_to", description = "DateTo to search for the hearings in.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @Size(max = 100) @Parameter(name = "event_text_contains", description = "Full or partial event_text", in = ParameterIn.QUERY) @Valid @RequestParam(value = "event_text_contains", required = false) String eventTextContains) {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber(StringUtils.trimToNull(caseNumber))
            .courthouse(StringUtils.trimToNull(courthouse))
            .courtroom(StringUtils.trimToNull(courtroom))
            .judgeName(StringUtils.trimToNull(judgeName))
            .defendantName(StringUtils.trimToNull(defendantName))
            .dateFrom(dateFrom).dateTo(dateTo)
            .eventTextContains(StringUtils.trimToNull(eventTextContains))
            .build();

        RequestValidator.validate(request);
        List<AdvancedSearchResult> advancedSearchResults = caseService.advancedSearch(request);
        return new ResponseEntity<>(advancedSearchResults, HttpStatus.OK);

    }

    @Override
    public ResponseEntity<List<Hearing>> casesCaseIdHearingsGet(
        @Parameter(name = "caseId", description = "caseId is the internal cas_id of the case.", required = true, in = ParameterIn.PATH) @PathVariable("caseId") Integer caseId) {

        return new ResponseEntity<>(caseService.getCaseHearings(caseId), HttpStatus.OK);
    }

    @Override
    // TODO: Should we also enforce @RolesAllowed() to coarsely screen by role before making any DB calls?
    public ResponseEntity<SingleCase> casesCaseIdGet(Integer caseId) {
        try {
            final List<CourthouseEntity> courthouses = List.of(caseRepository.getReferenceById(caseId).getCourthouse());
            authorisationApi.checkAuthorisation(courthouses);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Case-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(CASE_NOT_FOUND);
        }

        return new ResponseEntity<>(caseService.getCasesById(caseId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<EventResponse>> getEvents(
        @Parameter(name = "hearing_id", description = "hearingId is the internal hea_id of the hearing.", required = true, in = ParameterIn.PATH) @PathVariable("hearing_id") Integer hearingId) {
        return new ResponseEntity<>(caseService.getEvents(hearingId), HttpStatus.OK);
    }

}
