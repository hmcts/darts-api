package uk.gov.hmcts.darts.cases.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.cases.http.api.CasesApi;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchDetails;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PatchRequestObject;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.RequestValidator;
import uk.gov.hmcts.darts.cases.validator.PatchCaseRequestValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.CASE_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class CaseController implements CasesApi {

    private final CaseService caseService;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {DAR_PC})
    public ResponseEntity<List<ScheduledCase>> casesGet(
        String courthouse,
        String courtroom,
        LocalDate date
    ) {
        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(courthouse.toUpperCase(Locale.ROOT));
        request.setCourtroom(courtroom.toUpperCase(Locale.ROOT));
        request.setDate(date);

        return new ResponseEntity<>(caseService.getHearings(request), HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {MID_TIER})
    public ResponseEntity<PostCaseResponse> casesPost(AddCaseRequest addCaseRequest) {
        return new ResponseEntity<>(caseService.addCaseOrUpdate(addCaseRequest), HttpStatus.CREATED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<AdvancedSearchResult>> casesSearchPost(
        AdvancedSearchDetails advancedSearchDetails
    ) {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber(StringUtils.trimToNull(advancedSearchDetails.getCaseNumber()))
            .courthouse(StringUtils.trimToNull(advancedSearchDetails.getCourthouse()))
            .courtroom(StringUtils.trimToNull(advancedSearchDetails.getCourtroom()))
            .judgeName(StringUtils.trimToNull(advancedSearchDetails.getJudgeName()))
            .defendantName(StringUtils.trimToNull(advancedSearchDetails.getDefendantName()))
            .dateFrom(advancedSearchDetails.getDateFrom()).dateTo(advancedSearchDetails.getDateTo())
            .eventTextContains(StringUtils.trimToNull(advancedSearchDetails.getEventTextContains()))
            .build();

        RequestValidator.validate(request);
        List<AdvancedSearchResult> advancedSearchResults = caseService.advancedSearch(request);
        return new ResponseEntity<>(advancedSearchResults, HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, ADMIN, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, ADMIN, RCJ_APPEALS})
    public ResponseEntity<List<Hearing>> casesCaseIdHearingsGet(Integer caseId) {

        return new ResponseEntity<>(caseService.getCaseHearings(caseId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, ADMIN, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, ADMIN, RCJ_APPEALS})
    public ResponseEntity<SingleCase> casesCaseIdGet(Integer caseId) {

        return new ResponseEntity<>(caseService.getCasesById(caseId), HttpStatus.OK);

    }


    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, RCJ_APPEALS})
    public ResponseEntity<SingleCase> casesCaseIdPatch(Integer caseId, PatchRequestObject patchRequestObject) {
        PatchCaseRequestValidator.validate(patchRequestObject);
        return new ResponseEntity<>(caseService.patchCase(caseId, patchRequestObject), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, ADMIN, REQUESTER, APPROVER, TRANSCRIBER},
        globalAccessSecurityRoles = {JUDGE, ADMIN})
    public ResponseEntity<List<Transcript>> casesCaseIdTranscriptsGet(Integer caseId) {
        return new ResponseEntity<>(caseService.getTranscriptsByCaseId(caseId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, ADMIN},
        globalAccessSecurityRoles = {JUDGE, ADMIN})
    public ResponseEntity<List<Annotation>> getYourAnnotationsByCaseId(Integer caseId) {
        return new ResponseEntity<>(caseService.getAnnotations(caseId), HttpStatus.OK);
    }
}
