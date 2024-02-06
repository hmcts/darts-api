package uk.gov.hmcts.darts.hearings.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum HearingApiError implements DartsApiError {

    HEARING_NOT_FOUND(
          "100",
          HttpStatus.NOT_FOUND,
          "The requested hearing cannot be found"
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
