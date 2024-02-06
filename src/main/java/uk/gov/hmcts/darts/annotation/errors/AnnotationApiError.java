package uk.gov.hmcts.darts.annotation.errors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AnnotationApiError implements DartsApiError {

    HEARING_NOT_FOUND(
        "100",
        HttpStatus.NOT_FOUND,
        "The requested hearing cannot be found"
    ),

    FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT(
        "101",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "The annotation failed to be uploaded"
    ),

    ANNOTATION_NOT_FOUND(
        "102",
        HttpStatus.NOT_FOUND,
        "The requested annotation cannot be found"
    ),

    NOT_AUTHORISED_TO_DELETE(
            "102",
            HttpStatus.FORBIDDEN,
            "Not authorized to delete this annotation"
    );

    private static final String ERROR_TYPE_PREFIX = "ANNOTATION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
