package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.exception.AudioApiError.MEDIA_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class MediaIdValidatorTest {

    @Mock
    MediaRepository mediaRepository;

    private MediaIdValidator mediaIdValidator;

    @BeforeEach
    public void beforeTest() {
        mediaIdValidator = new MediaIdValidator(mediaRepository);
    }

    @Test
    void testValidateNotHiddenThrowsIfHidden() {
        Integer mediaId = 1;
        when(mediaRepository.existsByIdAndIsHiddenFalse(mediaId)).thenReturn(false);
        assertThatThrownBy(() -> mediaIdValidator.validateNotHidden(mediaId))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", MEDIA_NOT_FOUND);
    }

    @Test
    void testValidateNotHidden() {
        Integer mediaId = 1;
        when(mediaRepository.existsByIdAndIsHiddenFalse(mediaId)).thenReturn(true);
        mediaIdValidator.validateNotHidden(mediaId);
    }
}