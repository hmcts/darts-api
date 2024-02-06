package uk.gov.hmcts.darts.dailylist.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum DailyListError implements DartsApiError {

    FAILED_TO_PROCESS_DAILYLIST(
          "100",
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to process daily list"
    ),
    XML_OR_JSON_NEEDS_TO_BE_PROVIDED(
          "101",
          HttpStatus.BAD_REQUEST,
          "Either xml_document or json_document or both needs to be provided."
    ),
    XML_EXTRA_PARAMETERS_MISSING(
          "102",
          HttpStatus.BAD_REQUEST,
          "If xml_document is being provided without json_document, then courthouse, hearing_date, published_ts and unique_id also need to be provided."
    ),
    DAILY_LIST_NOT_FOUND(
          "103",
          HttpStatus.BAD_REQUEST,
          "The provided Daily List Id could not be found."
    ),
    INTERNAL_ERROR(
          "104",
          HttpStatus.INTERNAL_SERVER_ERROR,
          "An Internal Server Error has occurred."
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
