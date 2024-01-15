package uk.gov.hmcts.darts.retentions.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.retentions.http.api.RetentionsApi;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.service.RetentionsService;
import uk.gov.hmcts.darts.retentions.validation.RetentionsPostRequestValidator;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@RestController
@RequiredArgsConstructor
public class RetentionsController implements RetentionsApi {

    private final RetentionsService retentionsService;

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ContextIdEnum.CASE_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS, ADMIN},
        globalAccessSecurityRoles = {JUDGE, ADMIN})
    @Override
    public ResponseEntity<Void> retentionsPost(PostRetentionRequest postRetentionRequest) {
        RetentionsPostRequestValidator.validate(postRetentionRequest);
        retentionsService.postRetention(postRetentionRequest);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
