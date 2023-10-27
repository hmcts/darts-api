package uk.gov.hmcts.darts.transcriptions.validator;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_ATTACH_TRANSCRIPT;

@Component
public class TranscriptFileValidator {

    private static final long FILE_SIZE_LIMIT = 10000000;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("docx", "doc");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword"
    );

    public void validate(MultipartFile transcript) {
        if (transcript.getSize() >= FILE_SIZE_LIMIT) {
            throw new DartsApiException(FAILED_TO_ATTACH_TRANSCRIPT);
        }

        if (!ALLOWED_EXTENSIONS.contains(FilenameUtils.getExtension(transcript.getOriginalFilename()).toLowerCase())) {
            throw new DartsApiException(FAILED_TO_ATTACH_TRANSCRIPT);
        }

        if (!ALLOWED_CONTENT_TYPES.contains(transcript.getContentType())) {
            throw new DartsApiException(FAILED_TO_ATTACH_TRANSCRIPT);
        }
    }
}
