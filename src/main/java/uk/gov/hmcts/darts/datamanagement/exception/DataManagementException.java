package uk.gov.hmcts.darts.datamanagement.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
@Getter
@RequiredArgsConstructor
public enum DataManagementException implements DartsApiError {

    CHECHSUM_ERROR(
        "100",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Checksum error"
    );

    private static final String ERROR_TYPE_PREFIX = "DATA MANAGEMENT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }
}
