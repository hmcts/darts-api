package uk.gov.hmcts.darts.annotation.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDto;
import uk.gov.hmcts.darts.annotations.model.Annotation;

public interface AnnotationService {
    Integer process(MultipartFile document, Annotation annotation);

    AnnotationResponseDto downloadAnnotationDoc(Integer annotationId, Integer annotationDocumentId);


}
