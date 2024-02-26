package uk.gov.hmcts.darts.annotation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationDeleteService;
import uk.gov.hmcts.darts.common.component.validation.Validator;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationDeleteServiceImpl implements AnnotationDeleteService {

    private final AnnotationPersistenceService annotationPersistenceService;
    private final Validator<Integer> userAuthorisedToDeleteAnnotationValidator;
    private final Validator<Integer> annotationExistsValidator;

    @Override
    public void delete(Integer annotationId) {
        annotationExistsValidator.validate(annotationId);
        userAuthorisedToDeleteAnnotationValidator.validate(annotationId);
        annotationPersistenceService.markForDeletion(annotationId);
    }
}
