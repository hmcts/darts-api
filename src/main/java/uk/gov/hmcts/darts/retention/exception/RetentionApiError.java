package uk.gov.hmcts.darts.retention.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.retentions.model.RetentionErrorCode;
import uk.gov.hmcts.darts.retentions.model.RetentionTitleErrors;

@Getter
@RequiredArgsConstructor
public enum RetentionApiError implements DartsApiError {

    NO_PERMISSION_REDUCE_RETENTION(
            RetentionErrorCode.NO_PERMISSION_REDUCE_RETENTION.getValue(),
            HttpStatus.FORBIDDEN,
            RetentionTitleErrors.NO_PERMISSION_REDUCE_RETENTION.toString()
    ), RETENTION_DATE_TOO_EARLY(
            RetentionErrorCode.RETENTION_DATE_TOO_EARLY.getValue(),
            HttpStatus.UNPROCESSABLE_ENTITY,
            RetentionTitleErrors.RETENTION_DATE_TOO_EARLY.toString()
    ), INVALID_REQUEST(
            RetentionErrorCode.INVALID_REQUEST.getValue(),
            HttpStatus.BAD_REQUEST,
            RetentionTitleErrors.INVALID_REQUEST.toString()
    ), CASE_NOT_FOUND(
            RetentionErrorCode.CASE_NOT_FOUND.getValue(),
            HttpStatus.BAD_REQUEST,
            RetentionTitleErrors.CASE_NOT_FOUND.toString()
    ), CASE_NOT_CLOSED(
            RetentionErrorCode.CASE_NOT_CLOSED.getValue(),
            HttpStatus.BAD_REQUEST,
            RetentionTitleErrors.CASE_NOT_CLOSED.toString()
    ), NO_RETENTION_POLICIES_APPLIED(
            RetentionErrorCode.NO_RETENTION_POLICIES_APPLIED.getValue(),
            HttpStatus.BAD_REQUEST,
            RetentionTitleErrors.NO_RETENTION_POLICIES_APPLIED.toString()
    ), INTERNAL_SERVER_ERROR(
            RetentionErrorCode.INTERNAL_SERVER_ERROR.getValue(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            RetentionTitleErrors.INTERNAL_SERVER_ERROR.toString()
    ), RETENTION_DATE_TOO_LATE(
            RetentionErrorCode.RETENTION_DATE_TOO_LATE.getValue(),
            HttpStatus.UNPROCESSABLE_ENTITY,
            RetentionTitleErrors.RETENTION_DATE_TOO_LATE.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "RETENTION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }
}
