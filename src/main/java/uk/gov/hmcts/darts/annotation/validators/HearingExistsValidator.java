package uk.gov.hmcts.darts.annotation.validators;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.annotation.errors.AnnotationApiError;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

@RequiredArgsConstructor
@Component
public class HearingExistsValidator implements Validator<Annotation> {

    private final HearingRepository hearingRepository;

    @Override
    public void validate(Annotation annotation) {
        if (!hearingRepository.existsById(annotation.getHearingId())) {
            throw new DartsApiException(AnnotationApiError.HEARING_NOT_FOUND);
        }
    }
}
