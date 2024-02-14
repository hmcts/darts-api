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
    INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID_FOR_JUDGE(
            AnnotationErrorCode.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID_FOR_JUDGE.getValue(),
            HttpStatus.NOT_FOUND,
            AnnotationTitleErrors.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID_FOR_JUDGE.toString()
    ),
    INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID(
            AnnotationErrorCode.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID.getValue(),
            HttpStatus.FORBIDDEN,
            AnnotationTitleErrors.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID.toString()
    ),

    USER_NOT_AUTHORISED_TO_DOWNLOAD(
        AnnotationErrorCode.USER_NOT_AUTHORISED_TO_DOWNLOAD.getValue(),
        HttpStatus.FORBIDDEN,
        AnnotationTitleErrors.USER_NOT_AUTHORISED_TO_DOWNLOAD.toString()
    ),
    FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT(
            AnnotationErrorCode.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT.getValue(),
    HttpStatus.NOT_FOUND,
            AnnotationTitleErrors.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT.toString()
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
