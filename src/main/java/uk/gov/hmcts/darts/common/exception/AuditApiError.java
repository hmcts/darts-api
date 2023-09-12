package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.audio.model.AuditErrorCode;
import uk.gov.hmcts.darts.audio.model.AuditTitleErrors;

@Getter
@RequiredArgsConstructor
public enum AuditApiError implements DartsApiError {

    FILTERS_WERE_EMPTY(AuditErrorCode.AUDIT_FILTERS_WERE_EMPTY.getValue(),
        HttpStatus.BAD_REQUEST,
        AuditTitleErrors.FILTER_WAS_EMPTY.getValue()
    ),
    DATE_EMPTY(
        AuditErrorCode.AUDIT_DATE_EMPTY.getValue(),
        HttpStatus.BAD_REQUEST,
        AuditErrorCode.AUDIT_DATE_EMPTY.getValue()
    );

    private static final String ERROR_TYPE_PREFIX = "AUDIT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
