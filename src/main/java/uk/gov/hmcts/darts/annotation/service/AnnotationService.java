package uk.gov.hmcts.darts.annotation.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDTO;
import uk.gov.hmcts.darts.annotations.model.Annotation;

public interface AnnotationService {
    Integer process(MultipartFile document, Annotation annotation);

    AnnotationResponseDTO downloadAnnotationDoc(Integer annotationId, Integer annotationDocumentId);


}
