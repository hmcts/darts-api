package uk.gov.hmcts.darts.retention.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.retention.service.RetentionPostService;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.retention.validation.RetentionsPostRequestValidator;
import uk.gov.hmcts.darts.retentions.http.api.RetentionApi;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;
import uk.gov.hmcts.darts.retentions.model.GetRetentionPolicy;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.PostRetentionResponse;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.CASE_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class RetentionController implements RetentionApi {

    private final RetentionService retentionService;

    private final RetentionPostService retentionPostService;

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER})
    @Override
    public ResponseEntity<List<GetCaseRetentionsResponse>> retentionsGet(Integer caseId) {
        return new ResponseEntity<>(retentionService.getCaseRetentions(caseId), HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(
        contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    @Override
    public ResponseEntity<List<GetRetentionPolicy>> adminRetentionPolicyTypesGet() {
        return new ResponseEntity<>(retentionService.getRetentionPolicyTypes(), HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID, bodyAuthorisation = true,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER})
    @Override
    public ResponseEntity<PostRetentionResponse> retentionsPost(Boolean validateOnly,
                                                                PostRetentionRequest postRetentionRequest) {
        RetentionsPostRequestValidator.validate(postRetentionRequest);
        PostRetentionResponse response = retentionPostService.postRetention(validateOnly, postRetentionRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



}
