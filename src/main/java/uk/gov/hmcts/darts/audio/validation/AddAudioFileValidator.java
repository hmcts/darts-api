package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddAudioFileValidator implements Validator<MultipartFile> {

    private final AudioConfigurationProperties properties;

    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    public void validate(MultipartFile addAudioFileRequest) {
        if (addAudioFileRequest.getSize() <= 0) {
            throw new DartsApiException(AudioApiError.AUDIO_NOT_PROVIDED);
        }

        String contentType = addAudioFileRequest.getContentType();
        if (contentType != null && !properties.getAllowedMediaFormats().contains(contentType)) {
            log.warn("A file with format {} has been rejected.", contentType);
            throw new DartsApiException(AudioApiError.UNEXPECTED_FILE_TYPE);
        }

        String extension = FilenameUtils.getExtension(addAudioFileRequest.getOriginalFilename());
        if (!properties.getAllowedMediaExtensions().contains(extension)) {
            log.warn("A file with extension {} has been rejected.", extension);
            throw new DartsApiException(AudioApiError.UNEXPECTED_FILE_TYPE);
        }

        // check the file signature is suitable
        try {
            Tika tika = new Tika();
            String mimeType
                = tika.detect(addAudioFileRequest.getInputStream());

            if (!properties.getAllowedMediaMimeTypes().contains(mimeType)) {
                log.warn("A file with mimeType {} has been rejected.", mimeType);
                throw new DartsApiException(AudioApiError.UNEXPECTED_FILE_TYPE);
            }
        } catch (IOException ioException) {
            throw new DartsApiException(AudioApiError.UNEXPECTED_FILE_TYPE, ioException);
        }
    }

}