package uk.gov.hmcts.darts.annotation.service;

import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDto;

@FunctionalInterface
public interface AnnotationDownloadService {
    AnnotationResponseDto downloadAnnotationDoc(Integer annotationId, Integer annotationDocumentId);
}
