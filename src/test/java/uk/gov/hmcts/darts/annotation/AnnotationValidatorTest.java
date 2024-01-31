package uk.gov.hmcts.darts.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.annotation.component.AnnotationValidator;
import uk.gov.hmcts.darts.annotation.errors.AnnotationApiError;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationValidatorTest {

    @Mock
    private HearingRepository hearingRepository;

    private AnnotationValidator annotationValidator;

    @BeforeEach
    void setUp() {
        annotationValidator = new AnnotationValidator(hearingRepository);
    }

    @Test
    void throwsIfHearingNotFound() {
        when(hearingRepository.existsById(1)).thenReturn(false);

        assertThatThrownBy(() -> annotationValidator.validate(someAnnotationForHearingId(1)))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", AnnotationApiError.HEARING_NOT_FOUND);
    }

    @Test
    void doesntThrowIfHearingFound() {
        when(hearingRepository.existsById(1)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> annotationValidator.validate(someAnnotationForHearingId(1)));
    }

    private Annotation someAnnotationForHearingId(int hearingId) {
        Annotation annotation = new Annotation();
        annotation.setHearingId(hearingId);
        return annotation;
    }
}
