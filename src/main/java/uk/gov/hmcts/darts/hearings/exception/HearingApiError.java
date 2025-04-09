package uk.gov.hmcts.darts.hearings.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.hearings.model.HearingsErrorCode;
import uk.gov.hmcts.darts.hearings.model.HearingsTitleErrors;

@Getter
@RequiredArgsConstructor
public enum HearingApiError implements DartsApiError {
    HEARING_NOT_FOUND(
        HearingsErrorCode.HEARING_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        HearingsTitleErrors.HEARING_NOT_FOUND.toString()
    ),
    TOO_MANY_RESULTS(
        HearingsErrorCode.TOO_MANY_RESULTS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        HearingsTitleErrors.TOO_MANY_RESULTS.toString()
    ),
    HEARING_NOT_ACTUAL(
        HearingsErrorCode.HEARING_NOT_ACTUAL.getValue(),
        HttpStatus.NOT_FOUND,
        HearingsTitleErrors.HEARING_NOT_ACTUAL.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "HEARING";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}