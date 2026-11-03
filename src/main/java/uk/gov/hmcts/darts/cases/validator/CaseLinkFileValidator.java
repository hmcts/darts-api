package uk.gov.hmcts.darts.cases.validator;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

@Component
@RequiredArgsConstructor
public class CaseLinkFileValidator implements Validator<MultipartFile> {

    private final CaseLinkImportConfiguration configuration;

    @Override
    public void validate(MultipartFile file) {
        validateFileType(file);
        if (file.isEmpty()) {
            throw new DartsApiException(CaseApiError.EMPTY_FILE);
        }
        if (file.getSize() > configuration.getMaxFileSize().toBytes()) {
            throw new DartsApiException(CaseApiError.FILE_TOO_LARGE);
        }
    }

    private void validateFileType(MultipartFile file) {
        if (!"csv".equalsIgnoreCase(FilenameUtils.getExtension(file.getOriginalFilename()))) {
            throw new DartsApiException(CaseApiError.INVALID_FILE_TYPE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !"text/csv".equalsIgnoreCase(contentType.split(";", 2)[0].trim())) {
            throw new DartsApiException(CaseApiError.INVALID_CONTENT_TYPE);
        }
    }
}
