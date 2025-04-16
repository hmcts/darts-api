package uk.gov.hmcts.darts.annotation.errors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.annotations.model.AnnotationErrorCode;
import uk.gov.hmcts.darts.annotations.model.AnnotationTitleErrors;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AnnotationApiError implements DartsApiError {

    HEARING_NOT_FOUND(
        AnnotationErrorCode.HEARING_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AnnotationTitleErrors.HEARING_NOT_FOUND.toString()
    ),
    FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT(
        AnnotationErrorCode.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT.getValue(),
        HttpStatus.NOT_FOUND,
        AnnotationTitleErrors.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT.toString()
    ),
    USER_NOT_AUTHORISED_TO_DOWNLOAD(
        AnnotationErrorCode.USER_NOT_AUTHORISED_TO_DOWNLOAD.getValue(),
        HttpStatus.FORBIDDEN,
        AnnotationTitleErrors.USER_NOT_AUTHORISED_TO_DOWNLOAD.toString()
    ),
    INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID(
        AnnotationErrorCode.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID.getValue(),
        HttpStatus.NOT_FOUND,
        AnnotationTitleErrors.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID.toString()
    ),
    FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT(
        AnnotationErrorCode.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT.getValue(),
        HttpStatus.NOT_FOUND,
        AnnotationTitleErrors.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT.toString()
    ),
    INTERNAL_SERVER_ERROR(
        AnnotationErrorCode.INTERNAL_SERVER_ERROR.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AnnotationTitleErrors.INTERNAL_SERVER_ERROR.toString()
    ),
    BAD_REQUEST_DOC_TYPE(
        AnnotationErrorCode.BAD_REQUEST_DOC_TYPE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AnnotationTitleErrors.BAD_REQUEST_DOC_TYPE.toString()
    ),
    BAD_REQUEST_CONTENT_TYPE(
        AnnotationErrorCode.BAD_REQUEST_CONTENT_TYPE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AnnotationTitleErrors.BAD_REQUEST_CONTENT_TYPE.toString()
    ),
    ANNOTATION_NOT_FOUND(
        AnnotationErrorCode.ANNOTATION_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AnnotationTitleErrors.ANNOTATION_NOT_FOUND.toString()
    ),
    NOT_AUTHORISED_TO_DELETE(
        AnnotationErrorCode.NOT_AUTHORISED_TO_DELETE.getValue(),
        HttpStatus.FORBIDDEN,
        AnnotationTitleErrors.NOT_AUTHORISED_TO_DELETE.toString()
    ),

    BAD_REQUEST_FILE_SIZE(
        AnnotationErrorCode.BAD_REQUEST_FILE_SIZE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AnnotationTitleErrors.BAD_REQUEST_FILE_SIZE.toString()
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
