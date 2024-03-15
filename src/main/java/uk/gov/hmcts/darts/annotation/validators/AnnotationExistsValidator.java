package uk.gov.hmcts.darts.annotation.validators;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.ANNOTATION_NOT_FOUND;

@RequiredArgsConstructor
@Component
public class AnnotationExistsValidator implements Validator<Integer> {

    private final AnnotationRepository annotationRepository;

    @Override
    public void validate(Integer id) {
        if (!annotationRepository.existsById(id)) {
            throw new DartsApiException(ANNOTATION_NOT_FOUND);
        }
    }
}
