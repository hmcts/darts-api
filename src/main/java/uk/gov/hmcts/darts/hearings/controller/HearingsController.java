package uk.gov.hmcts.darts.hearings.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.hearings.http.api.HearingsApi;
import uk.gov.hmcts.darts.hearings.model.Annotation;
import uk.gov.hmcts.darts.hearings.model.EventResponse;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchRequest;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;
import uk.gov.hmcts.darts.hearings.model.Transcript;
import uk.gov.hmcts.darts.hearings.service.AdminHearingsService;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class HearingsController implements HearingsApi {

    private final HearingsService hearingsService;
    private final AdminHearingsService adminHearingSearch;

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA})
    @Override
    public ResponseEntity<GetHearingResponse> getHearing(Integer hearingId) {
        return new ResponseEntity<>(hearingsService.getHearings(hearingId), HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA})
    @Override
    public ResponseEntity<List<EventResponse>> getEvents(Integer hearingId) {
        return new ResponseEntity<>(hearingsService.getEvents(hearingId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS})
    public ResponseEntity<List<Transcript>> hearingsHearingIdTranscriptsGet(Integer hearingId) {
        return new ResponseEntity<>(hearingsService.getTranscriptsByHearingId(hearingId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN})
    public ResponseEntity<List<Annotation>> getHearingAnnotations(Integer hearingId) {
        return new ResponseEntity<>(hearingsService.getAnnotationsByHearingId(hearingId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<HearingsSearchResponse>> adminHearingsSearchPost(HearingsSearchRequest hearingsSearchRequest) {
        return new ResponseEntity<>(adminHearingSearch.adminHearingSearch(hearingsSearchRequest), HttpStatus.OK);
    }
}