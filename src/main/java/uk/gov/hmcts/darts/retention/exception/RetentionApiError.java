package uk.gov.hmcts.darts.retention.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum RetentionApiError implements DartsApiError {

    NO_PERMISSION_REDUCE_RETENTION_ERROR(
        "100",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "You do not have permission to reduce the retention period."
    ), RETENTION_DATE_INVALID_ERROR(
        "101",
        HttpStatus.BAD_REQUEST,
        "The retention date provided is invalid."
    ), INVALID_REQUEST(
        "102",
        HttpStatus.BAD_REQUEST,
        "The request is invalid."
    ), CASE_NOT_FOUND(
        "103",
        HttpStatus.BAD_REQUEST,
        "The requested caseId cannot be found."
    ), CASE_NOT_CLOSED(
        "104",
        HttpStatus.BAD_REQUEST,
        "The case must be closed before the retention period can be amended."
    ), NO_RETENTION_POLICIES_APPLIED(
        "105",
        HttpStatus.BAD_REQUEST,
        "The case must have a retention policy applied before being changed."
    ), RETENTION_DATE_TO_EARLY(
        "106",
        HttpStatus.BAD_REQUEST,
        "The retention date being applied is too early."
    ), NO_PERMISSIONS_REDUCE_DATE(
        "107",
        HttpStatus.BAD_REQUEST,
        "The case must have a retention policy applied before being changed."
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
