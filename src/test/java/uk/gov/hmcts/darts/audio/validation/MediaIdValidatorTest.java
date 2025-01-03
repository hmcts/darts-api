package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.exception.AudioApiError.MEDIA_NOT_FOUND;
import static uk.gov.hmcts.darts.audio.exception.AudioApiError.START_TIME_END_TIME_NOT_VALID;

@ExtendWith(MockitoExtension.class)
class MediaIdValidatorTest {

    @Mock
    MediaRepository mediaRepository;
    @Mock
    MediaEntity media1;

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

    @Test
    void validateNotZeroSecondAudio_shouldThrowException_ifStartAndEndAreEqual() {
        Integer mediaId = 1;
        OffsetDateTime now = OffsetDateTime.now();
        when(media1.getStart()).thenReturn(now);
        when(media1.getEnd()).thenReturn(now);
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media1));
        assertThatThrownBy(() -> mediaIdValidator.validateNotZeroSecondAudio(mediaId))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", START_TIME_END_TIME_NOT_VALID);
    }

    @Test
    void validateNotZeroSecondAudio_shouldThrowException_ifStartDateIsAfterAndEndDate() {
        Integer mediaId = 1;
        OffsetDateTime now = OffsetDateTime.now();
        when(media1.getStart()).thenReturn(now.plusSeconds(1));
        when(media1.getEnd()).thenReturn(now);
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media1));
        assertThatThrownBy(() -> mediaIdValidator.validateNotZeroSecondAudio(mediaId))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", START_TIME_END_TIME_NOT_VALID);
    }

    @Test
    void validateNotZeroSecondAudio_shouldThrowException_ifStartDateIsNull() {
        Integer mediaId = 1;
        when(media1.getStart()).thenReturn(null);
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media1));
        assertThatThrownBy(() -> mediaIdValidator.validateNotZeroSecondAudio(mediaId))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", START_TIME_END_TIME_NOT_VALID);
    }

    @Test
    void validateNotZeroSecondAudio_shouldThrowException_ifEndDateIsNull() {
        Integer mediaId = 1;
        OffsetDateTime now = OffsetDateTime.now();
        when(media1.getStart()).thenReturn(now);
        when(media1.getEnd()).thenReturn(null);
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media1));
        assertThatThrownBy(() -> mediaIdValidator.validateNotZeroSecondAudio(mediaId))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", START_TIME_END_TIME_NOT_VALID);
    }

    @Test
    void validateNotZeroSecondAudio_shouldNotThrowException_ifStartDateIsBeforeEndDate() {
        Integer mediaId = 1;
        OffsetDateTime now = OffsetDateTime.now();
        when(media1.getStart()).thenReturn(now.minusSeconds(1));
        when(media1.getEnd()).thenReturn(now);
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media1));
        assertThatNoException().isThrownBy(() -> mediaIdValidator.validateNotZeroSecondAudio(mediaId));
    }
}