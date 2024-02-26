package uk.gov.hmcts.darts.annotation.service;

import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDto;

public interface AnnotationDownloadService {
    AnnotationResponseDto downloadAnnotationDoc(Integer annotationId, Integer annotationDocumentId);
}
