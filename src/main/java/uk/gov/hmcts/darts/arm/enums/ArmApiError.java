package uk.gov.hmcts.darts.arm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.model.ArmErrorCode;
import uk.gov.hmcts.darts.common.model.ArmTitleErrors;

@Getter
@RequiredArgsConstructor
public enum ArmApiError implements DartsApiError {

    ARM_DOWN_FOR_MAINTENANCE(
        ArmErrorCode.ARM_DOWN_FOR_MAINTENTANCE.getValue(),
        HttpStatus.NOT_IMPLEMENTED,
        ArmTitleErrors.ARM_DOWN_FOR_MAINTENTANCE.getValue()
    );

    private static final String ERROR_TYPE_PREFIX = "ARM";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
