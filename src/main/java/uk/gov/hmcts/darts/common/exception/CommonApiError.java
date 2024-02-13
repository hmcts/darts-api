package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.model.CommonErrorCode;
import uk.gov.hmcts.darts.common.model.CommonTitleErrors;

@Getter
@RequiredArgsConstructor
public enum CommonApiError implements DartsApiError {

    COURTHOUSE_PROVIDED_DOES_NOT_EXIST(
        CommonErrorCode.COURTHOUSE_PROVIDED_DOES_NOT_EXIST.getValue(),
        HttpStatus.BAD_REQUEST,
        CommonTitleErrors.COURTHOUSE_PROVIDED_DOES_NOT_EXIST.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "COMMON";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
