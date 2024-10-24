package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

@Component
@RequiredArgsConstructor
public class MediaIdValidator implements Validator<Integer> {

    private final MediaRepository mediaRepository;

    @Override
    public void validate(Integer id) {
        if (mediaRepository.findByIdIncludeDeleted(id).isEmpty()) {
            throw new DartsApiException(AudioApiError.MEDIA_NOT_FOUND);
        }
    }
}