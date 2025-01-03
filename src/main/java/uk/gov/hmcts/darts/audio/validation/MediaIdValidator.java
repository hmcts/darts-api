package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MediaIdValidator implements Validator<Integer> {

    private final MediaRepository mediaRepository;

    public void validateNotHidden(Integer id) {
        if (!mediaRepository.existsByIdAndIsHiddenFalse(id)) {
            throw new DartsApiException(AudioApiError.MEDIA_NOT_FOUND);
        }
    }

    public void validateNotZeroSecondAudio(Integer id) {
        Optional<MediaEntity> media = mediaRepository.findById(id);
        media.ifPresent(e -> {
            if (e.getStart() == null || e.getEnd() == null || e.getStart().isEqual(e.getEnd()) || e.getStart().isAfter(e.getEnd())) {
                throw new DartsApiException(AudioApiError.START_TIME_END_TIME_NOT_VALID);
            }
        });
    }

    @Override
    public void validate(Integer id) {
        if (mediaRepository.findByIdIncludeDeleted(id).isEmpty()) {
            throw new DartsApiException(AudioApiError.MEDIA_NOT_FOUND);
        }
    }


}