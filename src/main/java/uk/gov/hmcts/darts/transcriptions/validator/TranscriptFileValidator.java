package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_ATTACH_TRANSCRIPT;

@Component
@RequiredArgsConstructor
public class TranscriptFileValidator {

    private final TranscriptionConfigurationProperties transcriptionConfigurationProperties;
    private final MultipartProperties multipartProperties;

    public void validate(MultipartFile transcript) {

        if (!transcriptionConfigurationProperties.getAllowedExtensions()
              .contains(FilenameUtils.getExtension(transcript.getOriginalFilename()).toLowerCase())
              || !transcriptionConfigurationProperties.getAllowedContentTypes()
              .contains(transcript.getContentType())
              || transcript.getSize() > multipartProperties.getMaxFileSize().toBytes()
        ) {
            throw new DartsApiException(FAILED_TO_ATTACH_TRANSCRIPT);
        }

    }
}
