package uk.gov.hmcts.darts.noderegistration.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum NodeRegistrationApiError implements DartsApiError {

    INVALID_COURTROOM(
          "100",
          HttpStatus.BAD_REQUEST,
          "Could not find the courtroom."
    );

    private static final String ERROR_TYPE_PREFIX = "NODE_REGISTRATION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
