package uk.gov.hmcts.darts.cases.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum CaseApiError implements DartsApiError {

    COURTHOUSE_PROVIDED_DOES_NOT_EXIST(
        "100",
        HttpStatus.BAD_REQUEST,
        "Provided courthouse does not exist"
    ),

    COURTROOM_PROVIDED_DOES_NOT_EXIST(
        "101",
        HttpStatus.BAD_REQUEST,
        "Provided courtroom does not exist"
    ),
    HEARING_MISMATCH(
        "102",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Could not find the correct hearing for provided case"
    );

    private static final String ERROR_TYPE_PREFIX = "SERVICE";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
