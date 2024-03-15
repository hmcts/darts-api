package uk.gov.hmcts.darts.noderegistration.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.noderegistration.model.NoDeregistrationErrorCode;
import uk.gov.hmcts.darts.noderegistration.model.NoDeregistrationTitleErrors;

@Getter
@RequiredArgsConstructor
public enum NodeRegistrationApiError implements DartsApiError {

    INVALID_COURTROOM(
        NoDeregistrationErrorCode.INVALID_COURTROOM.getValue(),
        HttpStatus.BAD_REQUEST,
        NoDeregistrationTitleErrors.INVALID_COURTROOM.toString()
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
