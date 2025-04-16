package uk.gov.hmcts.darts.annotation.service;

@FunctionalInterface
public interface AnnotationDeleteService {
    void delete(Integer annotationId);
}
