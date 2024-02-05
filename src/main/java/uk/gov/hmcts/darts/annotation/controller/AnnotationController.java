package uk.gov.hmcts.darts.annotation.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotations.http.api.AnnotationApi;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.CASE_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;


@RestController
@RequiredArgsConstructor
@Slf4j
public class AnnotationController implements AnnotationApi {

    private final AnnotationService annotationService;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = CASE_ID,
        securityRoles = {JUDGE, ADMIN},
        globalAccessSecurityRoles = {JUDGE, ADMIN})
    public ResponseEntity<List<Annotation>> getYourAnnotations(Integer caseId, Integer userId) {
        return new ResponseEntity<>(annotationService.getAnnotations(caseId, userId), HttpStatus.OK);
    }
}
