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
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.INVALID_REQUEST.toString()
    ), CASE_NOT_FOUND(
        RetentionErrorCode.CASE_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.CASE_NOT_FOUND.toString()
    ), CASE_NOT_CLOSED(
        RetentionErrorCode.CASE_NOT_CLOSED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.CASE_NOT_CLOSED.toString()
    ), NO_RETENTION_POLICIES_APPLIED(
        RetentionErrorCode.NO_RETENTION_POLICIES_APPLIED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.NO_RETENTION_POLICIES_APPLIED.toString()
    ), INTERNAL_SERVER_ERROR(
        RetentionErrorCode.INTERNAL_SERVER_ERROR.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        RetentionTitleErrors.INTERNAL_SERVER_ERROR.toString()
    ), RETENTION_DATE_TOO_LATE(
        RetentionErrorCode.RETENTION_DATE_TOO_LATE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.RETENTION_DATE_TOO_LATE.toString()
    ), RETENTION_POLICY_TYPE_ID_NOT_FOUND(
        RetentionErrorCode.RETENTION_POLICY_TYPE_ID_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        RetentionTitleErrors.RETENTION_POLICY_TYPE_ID_NOT_FOUND.toString()
    ), NON_UNIQUE_POLICY_NAME(
        RetentionErrorCode.NON_UNIQUE_POLICY_NAME.getValue(),
        HttpStatus.CONFLICT,
        RetentionTitleErrors.NON_UNIQUE_POLICY_NAME.toString()
    ), NON_UNIQUE_POLICY_DISPLAY_NAME(
        RetentionErrorCode.NON_UNIQUE_POLICY_DISPLAY_NAME.getValue(),
        HttpStatus.CONFLICT,
        RetentionTitleErrors.NON_UNIQUE_POLICY_DISPLAY_NAME.toString()
    ), DURATION_TOO_SHORT(
        RetentionErrorCode.DURATION_TOO_SHORT.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.DURATION_TOO_SHORT.toString()
    ), POLICY_START_MUST_BE_FUTURE(
        RetentionErrorCode.POLICY_START_MUST_BE_FUTURE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.POLICY_START_MUST_BE_FUTURE.toString()
    ), POLICY_START_DATE_MUST_BE_PAST(
        RetentionErrorCode.POLICY_START_DATE_MUST_BE_PAST.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.POLICY_START_DATE_MUST_BE_PAST.toString()
    ), NON_UNIQUE_FIXED_POLICY_KEY(
        RetentionErrorCode.NON_UNIQUE_FIXED_POLICY_KEY.getValue(),
        HttpStatus.CONFLICT,
        RetentionTitleErrors.NON_UNIQUE_FIXED_POLICY_KEY.toString()
    ), FIXED_POLICY_KEY_NOT_FOUND(
        RetentionErrorCode.FIXED_POLICY_KEY_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.FIXED_POLICY_KEY_NOT_FOUND.toString()
    ), LIVE_POLICIES_CANNOT_BE_EDITED(
        RetentionErrorCode.LIVE_POLICIES_CANNOT_BE_EDITED.getValue(),
        HttpStatus.CONFLICT,
        RetentionTitleErrors.LIVE_POLICIES_CANNOT_BE_EDITED.toString()
    ), TARGET_POLICY_HAS_PENDING_REVISION(
        RetentionErrorCode.TARGET_POLICY_HAS_PENDING_REVISION.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.TARGET_POLICY_HAS_PENDING_REVISION.toString()
    ), CASE_RETENTION_PASSED(
        RetentionErrorCode.CASE_RETENTION_PASSED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        RetentionTitleErrors.CASE_RETENTION_PASSED.toString()
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
