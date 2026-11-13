package uk.gov.hmcts.darts.cases.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.cases.model.CaseErrorCode;
import uk.gov.hmcts.darts.cases.model.CaseTitleErrors;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum CaseApiError implements DartsApiError {

    TOO_MANY_RESULTS(
        CaseErrorCode.TOO_MANY_RESULTS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.TOO_MANY_RESULTS.toString()

    ),
    NO_CRITERIA_SPECIFIED(
        CaseErrorCode.NO_CRITERIA_SPECIFIED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.NO_CRITERIA_SPECIFIED.toString()
    ),
    CRITERIA_TOO_BROAD(
        CaseErrorCode.CRITERIA_TOO_BROAD.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CRITERIA_TOO_BROAD.toString()
    ),
    INVALID_REQUEST(
        CaseErrorCode.INVALID_REQUEST.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.INVALID_REQUEST.toString()
    ),
    CASE_NOT_FOUND(
        CaseErrorCode.CASE_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        CaseTitleErrors.CASE_NOT_FOUND.toString()
    ),
    CASE_EXPIRED(
        CaseErrorCode.CASE_EXPIRED.getValue(),
        HttpStatus.NOT_FOUND,
        CaseTitleErrors.CASE_EXPIRED.toString()
    ),
    PATCH_CRITERIA_NOT_MET(
        CaseErrorCode.PATCH_CRITERIA_NOT_MET.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.PATCH_CRITERIA_NOT_MET.toString()
    ),
    HEARINGS_NOT_ACTUAL(
        CaseErrorCode.HEARINGS_NOT_ACTUAL.getValue(),
        HttpStatus.NOT_FOUND,
        CaseTitleErrors.HEARINGS_NOT_ACTUAL.toString()
    ),
    INVALID_FILE_TYPE(
        CaseErrorCode.INVALID_FILE_TYPE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.INVALID_FILE_TYPE.toString()
    ),
    INVALID_CONTENT_TYPE(
        CaseErrorCode.INVALID_CONTENT_TYPE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.INVALID_CONTENT_TYPE.toString()
    ),
    EMPTY_FILE(
        CaseErrorCode.EMPTY_FILE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.EMPTY_FILE.toString()
    ),
    FILE_TOO_LARGE(
        CaseErrorCode.FILE_TOO_LARGE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.FILE_TOO_LARGE.toString()
    ),
    MALFORMED_CSV(
        CaseErrorCode.MALFORMED_CSV.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.MALFORMED_CSV.toString()
    ),
    FILE_READ_FAILED(
        CaseErrorCode.FILE_READ_FAILED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.FILE_READ_FAILED.toString()
    ),
    INVALID_COLUMN_COUNT(
        CaseErrorCode.INVALID_COLUMN_COUNT.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.INVALID_COLUMN_COUNT.toString()
    ),
    CASE_1_CASE_NUMBER_REQUIRED(
        CaseErrorCode.CASE_1_CASE_NUMBER_REQUIRED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_1_CASE_NUMBER_REQUIRED.toString()
    ),
    CASE_1_COURTHOUSE_REQUIRED(
        CaseErrorCode.CASE_1_COURTHOUSE_REQUIRED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_1_COURTHOUSE_REQUIRED.toString()
    ),
    CASE_2_CASE_NUMBER_REQUIRED(
        CaseErrorCode.CASE_2_CASE_NUMBER_REQUIRED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_2_CASE_NUMBER_REQUIRED.toString()
    ),
    CASE_1_CASE_NUMBER_TOO_LONG(
        CaseErrorCode.CASE_1_CASE_NUMBER_TOO_LONG.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_1_CASE_NUMBER_TOO_LONG.toString()
    ),
    CASE_1_COURTHOUSE_TOO_LONG(
        CaseErrorCode.CASE_1_COURTHOUSE_TOO_LONG.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_1_COURTHOUSE_TOO_LONG.toString()
    ),
    CASE_2_CASE_NUMBER_TOO_LONG(
        CaseErrorCode.CASE_2_CASE_NUMBER_TOO_LONG.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_2_CASE_NUMBER_TOO_LONG.toString()
    ),
    DUPLICATE_MAPPING(
        CaseErrorCode.DUPLICATE_MAPPING.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.DUPLICATE_MAPPING.toString()
    ),
    COURTHOUSE_NOT_FOUND(
        CaseErrorCode.COURTHOUSE_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.COURTHOUSE_NOT_FOUND.toString()
    ),
    CASE_1_NOT_FOUND(
        CaseErrorCode.CASE_1_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CASE_1_NOT_FOUND.toString()
    ),
    SELF_LINK(
        CaseErrorCode.SELF_LINK.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.SELF_LINK.toString()
    ),
    TOO_MANY_ROWS(
        CaseErrorCode.TOO_MANY_ROWS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.TOO_MANY_ROWS.toString()
    ),
    CASE_LINK_IMPORT_NOT_FOUND(
        CaseErrorCode.CASE_LINK_IMPORT_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        CaseTitleErrors.CASE_LINK_IMPORT_NOT_FOUND.toString()
    ),
    CSV_RECORD_TOO_LARGE(
        CaseErrorCode.CSV_RECORD_TOO_LARGE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.CSV_RECORD_TOO_LARGE.toString()
    ),
    INVALID_FILE_COUNT(
        CaseErrorCode.INVALID_FILE_COUNT.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CaseTitleErrors.INVALID_FILE_COUNT.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "CASE";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
