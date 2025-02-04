package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddAudioFileValidator implements Validator<MultipartFile> {

    @Override
    public void validate(MultipartFile addAudioFileRequest) {
        if (addAudioFileRequest.getSize() <= 0) {
            throw new DartsApiException(AudioApiError.AUDIO_NOT_PROVIDED);
        }
    }

}