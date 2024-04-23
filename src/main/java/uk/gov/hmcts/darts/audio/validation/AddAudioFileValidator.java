package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddAudioFileValidator implements Validator<MultipartFile> {

    private final RetrieveCoreObjectService retrieveCoreObjectService;

    public void validate(MultipartFile addAudioMetadataRequest) {
        if (addAudioMetadataRequest.getSize() == 0) {
            throw new DartsApiException(AudioApiError.AUDIO_NOT_PROVIDED);
        }
    }
}