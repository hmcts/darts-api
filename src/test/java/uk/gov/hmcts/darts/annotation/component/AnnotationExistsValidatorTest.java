package uk.gov.hmcts.darts.annotation.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.annotation.errors.AnnotationApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationExistsValidatorTest {

    @Mock
    private AnnotationRepository annotationRepository;

    private AnnotationExistsValidator annotationExistsValidator;

    @BeforeEach
    void setUp() {
        annotationExistsValidator = new AnnotationExistsValidator(annotationRepository);
    }

    @Test
    void throwsIfHearingNotFound() {
        when(annotationRepository.existsById(1)).thenReturn(false);

        assertThatThrownBy(() -> annotationExistsValidator.validate(1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", AnnotationApiError.ANNOTATION_NOT_FOUND);
    }

    @Test
    void doesntThrowIfHearingFound() {
        when(annotationRepository.existsById(1)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> annotationExistsValidator.validate(1));
    }
}
