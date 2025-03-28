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
