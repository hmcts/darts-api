package uk.gov.hmcts.darts.annotation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDto;
import uk.gov.hmcts.darts.annotation.service.AnnotationDeleteService;
import uk.gov.hmcts.darts.annotation.service.AnnotationDownloadService;
import uk.gov.hmcts.darts.annotation.service.AnnotationUploadService;
import uk.gov.hmcts.darts.annotations.http.api.AnnotationsApi;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.annotations.model.PostAnnotationResponse;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANNOTATION_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnnotationController implements AnnotationsApi {

    private final AnnotationDownloadService downloadService;
    private final AnnotationDeleteService deleteService;
    private final AnnotationUploadService uploadService;

    @Authorisation(
        bodyAuthorisation = true,
        contextId = HEARING_ID,
        securityRoles = {JUDGE},
        globalAccessSecurityRoles = {JUDGE})
    @Override
    public ResponseEntity<PostAnnotationResponse> postAnnotation(MultipartFile file, Annotation annotation) {
        var annotationId = uploadService.upload(file, annotation);
        return ResponseEntity.ok(new PostAnnotationResponse(annotationId));
    }

    @Authorisation(
            contextId = ANNOTATION_ID,
            securityRoles = {JUDGE},
            globalAccessSecurityRoles = {JUDGE, ADMIN})
    @Override
    public ResponseEntity<Void> deleteAnnotation(Integer annotationId) {
        deleteService.delete(annotationId);
        return ResponseEntity.noContent().build();
    }

    @Authorisation(
            contextId = ANNOTATION_ID,
            securityRoles = {JUDGE, ADMIN},
            globalAccessSecurityRoles = {JUDGE, ADMIN})
    @Override
    public ResponseEntity<Resource> downloadAnnotation(Integer annotationId, Integer annotationDocumentId) {

        final AnnotationResponseDto annotationResponseDto = downloadService.downloadAnnotationDoc(annotationId, annotationDocumentId);

        return ResponseEntity.ok()
                .header(
                        CONTENT_DISPOSITION,
                        String.format("attachment; filename=\"%s\"", annotationResponseDto.getFileName())
                )
                .header(
                        "external_location",
                        String.valueOf(annotationResponseDto.getExternalLocation())
                )
                .header(
                        "annotation_document_id",
                        String.valueOf(annotationResponseDto.getAnnotationDocumentId())
                )
                .body(annotationResponseDto.getResource());
    }
}
