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
    ),
    FEATURE_FLAG_NOT_ENABLED(
        CommonErrorCode.FEATURE_FLAG_NOT_ENABLED.getValue(),
        HttpStatus.NOT_IMPLEMENTED,
        CommonTitleErrors.FEATURE_FLAG_NOT_ENABLED.getValue()
    ),
    NOT_FOUND(
        CommonErrorCode.NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        CommonTitleErrors.NOT_FOUND.getValue()
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
