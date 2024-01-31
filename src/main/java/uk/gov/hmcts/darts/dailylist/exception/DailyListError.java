package uk.gov.hmcts.darts.dailylist.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.dailylist.model.DailyListErrorCode;
import uk.gov.hmcts.darts.dailylist.model.DailyListTitleErrors;

@Getter
@RequiredArgsConstructor
public enum DailyListError implements DartsApiError {

    FAILED_TO_PROCESS_DAILYLIST(
        DailyListErrorCode.FAILED_TO_PROCESS_DAILYLIST.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        DailyListTitleErrors.FAILED_TO_PROCESS_DAILYLIST.toString()
    ),
    XML_OR_JSON_NEEDS_TO_BE_PROVIDED(
        DailyListErrorCode.XML_OR_JSON_NEEDS_TO_BE_PROVIDED.getValue(),
        HttpStatus.BAD_REQUEST,
        DailyListTitleErrors.XML_OR_JSON_NEEDS_TO_BE_PROVIDED.toString()
    ),
    XML_EXTRA_PARAMETERS_MISSING(
        DailyListErrorCode.XML_EXTRA_PARAMETERS_MISSING.getValue(),
        HttpStatus.BAD_REQUEST,
        DailyListTitleErrors.XML_EXTRA_PARAMETERS_MISSING.toString()
    ),
    DAILY_LIST_NOT_FOUND(
        DailyListErrorCode.DAILY_LIST_NOT_FOUND.getValue(),
        HttpStatus.BAD_REQUEST,
        DailyListTitleErrors.DAILY_LIST_NOT_FOUND.toString()
    ),
    INTERNAL_ERROR(
        DailyListErrorCode.INTERNAL_ERROR.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        DailyListTitleErrors.INTERNAL_ERROR.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "DAILYLIST";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }
}
