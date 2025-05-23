package uk.gov.hmcts.darts.cases.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.http.api.CasesApi;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.cases.model.AdminCasesIdAudiosGet200Response;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.cases.model.AdminSingleCaseResponseItem;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.cases.model.CasesCaseIdEventsGet200Response;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.cases.service.AdminCaseService;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.RequestValidator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.PostAdminSearchRequest;
import uk.gov.hmcts.darts.common.util.AdminSearchRequestValidator;
import uk.gov.hmcts.darts.common.util.CourtValidationUtils;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.util.DataUtil;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.time.LocalDate;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.CASE_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DARTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class CaseController implements CasesApi {

    private final CaseService caseService;
    private final AdminCaseService adminCaseService;

    private final LogApi logApi;
    private final AdminSearchRequestValidator adminSearchRequestValidator;

    @Value("${darts.log.cases.defendant-name-char-limit: 600}")
    private int limit;

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
        request.setCourthouse(DataUtil.toUpperCase(courthouse));
        request.setCourtroom(DataUtil.toUpperCase(courtroom));
        request.setDate(date);

        return new ResponseEntity<>(caseService.getHearings(request), HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {MID_TIER})
    public ResponseEntity<PostCaseResponse> casesAddCasePost(AddCaseRequest addCaseRequest) {
        DataUtil.preProcess(addCaseRequest);
        validateRequest(addCaseRequest);
        return new ResponseEntity<>(caseService.addCaseOrUpdate(addCaseRequest), HttpStatus.CREATED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {XHIBIT})
    public ResponseEntity<PostCaseResponse> casesAddDocumentPost(AddCaseRequest addCaseRequest) {
        return casesAddCasePost(addCaseRequest);
    }

    private void validateRequest(AddCaseRequest addCaseRequest) {
        emptyIfNull(addCaseRequest.getDefendants()).forEach(newDefendant -> {
            if (newDefendant.length() > limit) {
                logApi.defendantNameOverflow(addCaseRequest);
            }
        });
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<AdvancedSearchResult>> casesSearchPost(
        AdvancedSearchRequest advancedSearchRequest
    ) {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber(advancedSearchRequest.getCaseNumber())
            .courthouseIds(advancedSearchRequest.getCourthouseIds())
            .courtroom(StringUtils.trimToNull(advancedSearchRequest.getCourtroom()))
            .judgeName(StringUtils.trimToNull(advancedSearchRequest.getJudgeName()))
            .defendantName(StringUtils.trimToNull(advancedSearchRequest.getDefendantName()))
            .dateFrom(advancedSearchRequest.getDateFrom()).dateTo(advancedSearchRequest.getDateTo())
            .eventTextContains(StringUtils.trimToNull(advancedSearchRequest.getEventTextContains()))
            .build();

        RequestValidator.validate(request);
        List<AdvancedSearchResult> advancedSearchResults = caseService.advancedSearch(request);
        return new ResponseEntity<>(advancedSearchResults, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<List<Hearing>> casesCaseIdHearingsGet(Integer caseId) {

        return new ResponseEntity<>(caseService.getCaseHearings(caseId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<CasesCaseIdEventsGet200Response> casesCaseIdEventsGet(
        Integer caseId,
        Integer pageNumber,
        Integer pageSize,
        List<String> sortBy,
        List<String> sortOrder
    ) {
        PaginationDto<Event> paginationDto = new PaginationDto<>(
            pageNumber,
            pageSize,
            PaginationDto.toSortBy(sortBy),
            PaginationDto.toSortDirection(sortOrder)
        );

        return new ResponseEntity<>(
            caseService.getEventsByCaseId(caseId, paginationDto)
                .mapToPaginatedListCommon(new CasesCaseIdEventsGet200Response(), CasesCaseIdEventsGet200Response::setData),
            HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<SingleCase> casesCaseIdGet(Integer caseId) {

        return new ResponseEntity<>(caseService.getCasesById(caseId), HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<List<Transcript>> casesCaseIdTranscriptsGet(Integer caseId) {
        return new ResponseEntity<>(caseService.getTranscriptsByCaseId(caseId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDICIARY},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, DARTS})
    public ResponseEntity<List<Annotation>> getYourAnnotationsByCaseId(Integer caseId) {
        return new ResponseEntity<>(caseService.getAnnotations(caseId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_USER, SUPER_ADMIN})
    public ResponseEntity<List<AdminCasesSearchResponseItem>> adminCasesSearchPost(AdminCasesSearchRequest adminCasesSearchRequest) {
        adminSearchRequestValidator.validate(PostAdminSearchRequest.builder()
                                                 .caseNumber(adminCasesSearchRequest.getCaseNumber())
                                                 .courthouseIds(adminCasesSearchRequest.getCourthouseIds())
                                                 .hearingStartAt(adminCasesSearchRequest.getHearingStartAt())
                                                 .hearingEndAt(adminCasesSearchRequest.getHearingEndAt())
                                                 .build());
        validateUppercase(null, adminCasesSearchRequest.getCourtroomName());
        return new ResponseEntity<>(caseService.adminCaseSearch(adminCasesSearchRequest), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_USER, SUPER_ADMIN})
    public ResponseEntity<AdminSingleCaseResponseItem> adminCasesIdGet(Integer id) {
        return new ResponseEntity<>(caseService.adminGetCaseById(id), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_USER, SUPER_ADMIN})
    public ResponseEntity<AdminCasesIdAudiosGet200Response> adminCasesIdAudiosGet(
        Integer caseId,
        Integer pageNumber,
        Integer pageSize,
        List<String> sortBy,
        List<String> sortOrder
    ) {
        PaginationDto<AdminCaseAudioResponseItem> paginationDto = new PaginationDto<>(
            PaginatedList<AdminCaseAudioResponseItem>::new,
            pageNumber,
            pageSize,
            PaginationDto.toSortBy(sortBy),
            PaginationDto.toSortDirection(sortOrder)
        );

        return new ResponseEntity<>(
            adminCaseService.getAudiosByCaseId(caseId, paginationDto)
                .mapToPaginatedListCommon(new AdminCasesIdAudiosGet200Response(), AdminCasesIdAudiosGet200Response::setData), HttpStatus.OK);

    }

    void validateUppercase(String courthouse, String courtroom) {
        if (!CourtValidationUtils.isUppercase(courthouse, courtroom)) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "Courthouse and courtroom must be uppercase.");
        }
    }

}
