package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.audit.model.AuditErrorCode;
import uk.gov.hmcts.darts.audit.model.AuditTitleErrors;

@Getter
@RequiredArgsConstructor
public enum AuditApiError implements DartsApiError {

    FILTERS_WERE_EMPTY(
        AuditErrorCode.AUDIT_FILTERS_WERE_EMPTY.getValue(),
        HttpStatus.BAD_REQUEST,
        AuditTitleErrors.AUDIT_FILTERS_WERE_EMPTY.toString()
    ),
    DATE_EMPTY(
        AuditErrorCode.FILTER_EMPTY.getValue(),
        HttpStatus.BAD_REQUEST,
        AuditTitleErrors.FILTER_EMPTY.toString()
    ),
    NO_HEARING_OR_USER_FOUND_WHEN_ADDING_AUDIO_AUDIT(
        AuditErrorCode.NO_HEARING_OR_USER_FOUND_WHEN_ADDING_AUDIO_AUDIT.getValue(),
        HttpStatus.BAD_REQUEST,
        AuditTitleErrors.NO_HEARING_OR_USER_FOUND_WHEN_ADDING_AUDIO_AUDIT.toString()
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
