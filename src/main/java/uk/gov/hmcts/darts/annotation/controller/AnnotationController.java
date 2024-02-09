package uk.gov.hmcts.darts.annotation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotations.http.api.AnnotationsApi;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.annotations.model.PostAnnotationResponse;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnnotationController implements AnnotationsApi {

    private final AnnotationService annotationService;

    @Authorisation(
        bodyAuthorisation = true,
        contextId = HEARING_ID,
        securityRoles = {JUDGE},
        globalAccessSecurityRoles = {JUDGE})
    @Override
    public ResponseEntity<PostAnnotationResponse> annotationsPost(MultipartFile file, Annotation annotation) {
        var annotationId = annotationService.process(file, annotation);
        return ResponseEntity.ok(new PostAnnotationResponse(annotationId));
    }
}
