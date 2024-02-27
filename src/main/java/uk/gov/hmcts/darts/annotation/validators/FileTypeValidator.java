package uk.gov.hmcts.darts.annotation.validators;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.config.AnnotationConfigurationProperties;
import uk.gov.hmcts.darts.annotation.errors.AnnotationApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FilenameUtils.getExtension;

@RequiredArgsConstructor
@Component
public class FileTypeValidator implements Validator<MultipartFile> {

    private final AnnotationConfigurationProperties config;

    @Override
    public void validate(MultipartFile file) {
        var uploadedFileExtension = requireNonNull(getExtension(file.getOriginalFilename())).toLowerCase();
        if (!config.getAllowedExtensions().contains(uploadedFileExtension)) {
            throw new DartsApiException(AnnotationApiError.BAD_REQUEST_DOC_TYPE);
        }
        if (!config.getAllowedContentTypes().contains(file.getContentType())) {
            throw new DartsApiException(AnnotationApiError.BAD_REQUEST_CONTENT_TYPE);
        }
        if (getBytes(file).length > config.getMaxFileSize()) {
            throw new DartsApiException(AnnotationApiError.BAD_REQUEST_FILE_SIZE);
        }
    }

    private static byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new DartsApiException(AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }
    }
}
