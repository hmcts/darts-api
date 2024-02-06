package uk.gov.hmcts.darts.retention.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.retention.service.RetentionPostService;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.retention.validation.RetentionsPostRequestValidator;
import uk.gov.hmcts.darts.retentions.http.api.RetentionApi;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.PostRetentionResponse;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@RestController
@RequiredArgsConstructor
public class RetentionController implements RetentionApi {

    private final RetentionService retentionService;

    private final RetentionPostService retentionPostService;

    @Override
    public ResponseEntity<List<GetCaseRetentionsResponse>> retentionsGet(Integer caseId) {
        return new ResponseEntity<>(retentionService.getCaseRetentions(caseId), HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ContextIdEnum.CASE_ID, bodyAuthorisation = true,
            securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, ADMIN},
            globalAccessSecurityRoles = {JUDGE, ADMIN})
    @Override
    public ResponseEntity<PostRetentionResponse> retentionsPost(PostRetentionRequest postRetentionRequest) {
        RetentionsPostRequestValidator.validate(postRetentionRequest);
        PostRetentionResponse response = retentionPostService.postRetention(postRetentionRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
