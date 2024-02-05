package uk.gov.hmcts.darts.annotation.service;

import uk.gov.hmcts.darts.annotations.model.Annotation;

import java.util.List;

public interface AnnotationService {
    public List<Annotation> getAnnotations(Integer caseId, Integer userId);
}
