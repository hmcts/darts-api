package uk.gov.hmcts.darts.annotation.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotations.model.Annotation;

public interface AnnotationUploadService {
    Integer upload(MultipartFile multipartFile, Annotation annotation);
}
